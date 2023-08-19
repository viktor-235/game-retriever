package com.github.viktor235.gameretriever.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.viktor235.gameretriever.exception.AppException;
import com.github.viktor235.gameretriever.model.converter.Converter;
import com.github.viktor235.gameretriever.model.converter.Handler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ConverterService {

    private final ObjectMapper objectMapper;

    private Map<String, Converter> converters;

    private static final String CONVERTER_DIR = "converters";
    private static final int REGEX_FLAGS = 0;

    @PostConstruct
    private void init() throws AppException {
        readConverterFiles();
    }

    public Map<String, Converter> getConverters() {
        readConverterFiles();
        return converters;
    }

    private void readConverterFiles() throws AppException {
        converters = new LinkedHashMap<>();
        try {
            for (final File fileEntry : getFilesByExtension(CONVERTER_DIR, "json")) {
                if (!fileEntry.isDirectory()) {
                    Converter converter = objectMapper.readValue(fileEntry, Converter.class);
                    converters.put(removeExtension(fileEntry.getName()), converter);
                }
            }
        } catch (IOException e) {
            throw new AppException("Error while reading converter files: " + e.getMessage(), e);
        }
    }

    private File[] getFilesByExtension(String folderPath, String extension) {
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdir();
        }
        if (!folder.exists() || !folder.isDirectory()) {
            throw new AppException("Converter folder '%s' not found".formatted(CONVERTER_DIR));
        }
        return folder.listFiles((dir, name) ->
                name.toLowerCase().endsWith("." + extension)
        );
    }

    private String removeExtension(String fileName) {
        int lastPeriodPos = fileName.lastIndexOf('.');
        if (lastPeriodPos > 0)
            fileName = fileName.substring(0, lastPeriodPos);
        return fileName;
    }

    public void convert(String converterName, Consumer<String> progressCallback) {
        Converter converterCfg = converters.get(converterName);
        if (converterCfg == null) {
            throw new AppException("Unknown SQL converter '%s'".formatted(converterName));
        }
        convert(converterCfg, progressCallback);
    }

    public void convert(Converter converterCfg,
                        Consumer<String> progressCallback) {
        if (CollectionUtils.isEmpty(converterCfg.getHandlers())) {
            throw new AppException("Converter does not contain handlers");
        }

        String inputFile = converterCfg.getInputFile();
        String outputFile = converterCfg.getOutputFile();

        try (PrintWriter outputStream = new PrintWriter(outputFile)) {
            int handlerIndex = 0;
            for (Handler handler : converterCfg.getHandlers()) {
                progressCallback.accept("(handler %d/%d) %s".formatted(
                        ++handlerIndex, converterCfg.getHandlers().size(), handler.getName()));

                handle(inputFile, outputStream, handler);
            }
        } catch (IOException e) {
            throw new AppException("Error while converting '%s' -> '%s': %s".formatted(inputFile, outputFile, e.getMessage()), e);
        }
    }

    private void handle(String inputFile, PrintWriter outputStream, Handler handler) throws IOException {
        if (handler.getPattern() == null || handler.getSubstitution() == null) {
            return;
        }

        try (BufferedReader input = new BufferedReader(new FileReader(inputFile))) {
            Pattern filter = handler.getFilter() == null ? null : Pattern.compile(handler.getFilter(), REGEX_FLAGS);
            Pattern pattern = Pattern.compile(handler.getPattern(), REGEX_FLAGS);

            String line;
            while ((line = input.readLine()) != null) {
                if (!suitableLine(line, filter)) {
                    continue;
                }

                Matcher matcher = pattern.matcher(line);
                if (!matcher.matches()) {
                    continue;
                }

                String newLine = matcher.replaceAll(handler.getSubstitution());
                outputStream.println(newLine);
            }
            outputStream.println();
        }
    }

    private boolean suitableLine(String line, Pattern filter) {
        if (filter == null) {
            return true;
        }
        return filter.matcher(line).find();
    }
}
