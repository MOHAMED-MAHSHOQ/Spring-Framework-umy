package guru.springframework.spring6restmvc.build;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OpenApiJsonToXmlConverter {

    public static void main(String[] args) throws IOException {
        Path inputPath = Paths.get(args.length > 0 ? args[0] : "target/openapi/openapi.json");
        Path outputPath = Paths.get(args.length > 1 ? args[1] : "target/openapi/openapi.xml");

        if (!Files.exists(inputPath)) {
            throw new IllegalStateException("OpenAPI JSON file not found: " + inputPath.toAbsolutePath());
        }

        Files.createDirectories(outputPath.getParent());

        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode root = jsonMapper.readTree(inputPath.toFile());

        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        xmlMapper.writer().withRootName("openapi").writeValue(outputPath.toFile(), root);

        System.out.println("Generated OpenAPI XML: " + outputPath.toAbsolutePath());
    }
}

