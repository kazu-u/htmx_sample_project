package com.example.htmx.examples.fileupload;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

/** アップロード例で共通するテキストファイルの制約を検証する。 */
final class UploadedTextFileValidator {
    static final long MAX_FILE_SIZE = 1024L * 1024L;
    static final int MAX_FILE_NAME_LENGTH = 120;

    enum Problem {
        EMPTY("UTF-8 のテキストファイルを1件選択してください。", 422),
        TOO_LARGE("送信内容が上限を超えています。ファイルサイズを確認してください。", 413),
        INVALID_NAME("ファイル名は120文字以内で指定してください。", 422),
        INVALID_EXTENSION("拡張子が .txt のファイルを選択してください。", 422),
        INVALID_CONTENT("ファイルの内容が UTF-8 のテキストではありません。", 422);

        private final String message;
        private final int statusCode;

        Problem(String message, int statusCode) {
            this.message = message;
            this.statusCode = statusCode;
        }

        String message() {
            return message;
        }

        int statusCode() {
            return statusCode;
        }
    }

    private UploadedTextFileValidator() {}

    static Optional<Problem> validate(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Optional.of(Problem.EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return Optional.of(Problem.TOO_LARGE);
        }
        String fileName = displayName(file.getOriginalFilename());
        if (fileName.isBlank() || fileName.codePointCount(0, fileName.length()) > MAX_FILE_NAME_LENGTH) {
            return Optional.of(Problem.INVALID_NAME);
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            return Optional.of(Problem.INVALID_EXTENSION);
        }
        if (!isUtf8Text(file.getBytes())) {
            return Optional.of(Problem.INVALID_CONTENT);
        }
        return Optional.empty();
    }

    static String displayName(String originalName) {
        if (originalName == null) {
            return "";
        }
        String basename = originalName.replace('\\', '/');
        basename = basename.substring(basename.lastIndexOf('/') + 1);
        return basename.codePoints()
                .filter(codePoint -> !Character.isISOControl(codePoint))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private static boolean isUtf8Text(byte[] contents) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(contents))
                    .toString();
            return text.codePoints().noneMatch(codePoint -> codePoint == 0
                    || (Character.isISOControl(codePoint)
                    && codePoint != '\n' && codePoint != '\r' && codePoint != '\t'));
        } catch (CharacterCodingException exception) {
            return false;
        }
    }
}
