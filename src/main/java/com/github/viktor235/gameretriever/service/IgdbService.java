package com.github.viktor235.gameretriever.service;

import com.api.igdb.apicalypse.APICalypse;
import com.api.igdb.exceptions.RequestException;
import com.api.igdb.request.IGDBWrapper;
import com.api.igdb.request.ProtoRequestKt;
import com.github.viktor235.gameretriever.exception.AppException;
import com.github.viktor235.gameretriever.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import proto.Game;
import proto.Platform;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.api.igdb.request.ProtoRequestKt.games;

@Service
@RequiredArgsConstructor
public class IgdbService {

    private final IGDBWrapper api = IGDBWrapper.INSTANCE;

    /**
     * 500 is max limit value (see <a href="https://api-docs.igdb.com/#pagination">IGDB API Docs</a>)
     */
    private final static int BUFFER_SIZE = 500;

    public void setCredentials(String clientId, String token) {
        api.setCredentials(clientId, token);
    }

    public boolean isAuth() throws AppException {
        try {
            games(api, new APICalypse().where("id=" + 5601));
            return true;
        } catch (RequestException e) {
            if (HttpStatus.UNAUTHORIZED.value() == e.getStatusCode()) {
                return false;
            } else {
                throw prepareException(e);
            }
        }
    }

    public void getPlatforms(Consumer<List<Platform>> handleBufferFunc) throws AppException {
        APICalypse query = new APICalypse()
                .fields("name,abbreviation");
        requestAndIterate(ProtoRequestKt::platforms, query, handleBufferFunc);
    }

    public void getGames(long platformId, Consumer<List<Game>> handleBufferFunc) throws AppException {
        APICalypse query = new APICalypse()
                .where("platforms=" + platformId)
                .fields("name, url");
        requestAndIterate(ProtoRequestKt::games, query, handleBufferFunc);
    }

    @Deprecated
    public int getGameCount(long platformId) throws AppException {
        // IGDB-API-JVM has no '/games/count' endpoint, so I used ineffective hack
        APICalypse query = new APICalypse().where("platforms=" + platformId);
        AtomicInteger count = new AtomicInteger();
        requestAndIterate(ProtoRequestKt::externalGames, query,
                (buffer) -> count.addAndGet(buffer.size())
        );
        return count.get();
    }

    /**
     * Requests data from IGDB API. Uses buffer to handle huge requests
     *
     * @param request          API function specified in {@link ProtoRequestKt}
     * @param query            APICalypse API query. This method adds <code>step</code> and <code>limit</code> to the query
     * @param handleBufferFunc function to handle result data buffer. This function calls many times if a lot of data requested
     * @param <T>              type of result data
     * @throws AppException when error while data requesting
     */
    private <T> void requestAndIterate(
            ThrowingBiFunction<IGDBWrapper, APICalypse, List<T>> request,
            APICalypse query,
            Consumer<List<T>> handleBufferFunc
    ) throws AppException {
        int offset = 0;
        int step = BUFFER_SIZE;
        query.limit(step);
        boolean running = true;
        do {
            List<T> buffer;
            try {
                buffer = request.apply(api, query.offset(offset));
            } catch (RuntimeException e) {
                if (e.getCause() instanceof RequestException cause) {
                    throw prepareException(cause);
                } else {
                    throw new AppException("Unexpected error while requesting IGDB API: " + e.getMessage(), e);
                }
            }

            if (buffer.isEmpty()) {
                running = false;
            } else {
                handleBufferFunc.accept(buffer);
                offset += step;
            }
        } while (running);
    }

    private AuthException prepareException(RequestException e) {
        int statusCode = e.getStatusCode();
        String err = switch (statusCode) {
            case 401 -> "Unauthorized. Use 'auth' command to authorize to the IGDB";
            default -> "Error while IGDB API request. HTTP code: " + statusCode;
        };
        return new AuthException(err, e);
    }

    interface ThrowingBiFunction<T1, T2, R> {

        R applyWithException(T1 t1, T2 t2) throws Exception;

        default R apply(T1 t1, T2 t2) {
            try {
                return applyWithException(t1, t2);
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }
}
