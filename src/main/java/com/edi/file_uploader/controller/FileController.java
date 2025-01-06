package com.edi.file_uploader.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final String uploadDir = "C:/uploads/";  // Caminho absoluto para o diretório de uploads

    public FileController() {
        // Cria o diretório de uploads caso não exista
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                System.out.println("Diretório de upload criado: " + uploadDir);
            } else {
                System.out.println("Falha ao criar o diretório de upload: " + uploadDir);
            }
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Nenhum arquivo enviado.");
        }

        try {
            // Recupera o arquivo temporário gerado pelo Tomcat
            String tempFilePath = System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename();
            Path tempFile = Path.of(tempFilePath);
            
            // Salva o arquivo temporário no diretório
            Files.write(tempFile, file.getBytes());

            // Caminho do arquivo final
            Path finalFile = Path.of(uploadDir + file.getOriginalFilename());

            // Move o arquivo temporário para o diretório de upload
            Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);

            // Processa o conteúdo do arquivo
            String content = new String(file.getBytes());
            String processedContent = processContent(content);

            // Retorna o conteúdo processado
            return ResponseEntity.ok("Arquivo salvo com sucesso em: " + finalFile.toString() + "\nConteúdo Processado:\n" + processedContent);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao mover o arquivo: " + e.getMessage());
        }
    }

    private String processContent(String content) {
        String[] lines = content.split("\\r?\\n"); // Divide o conteúdo em linhas
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            // Verifica se a linha tem pelo menos 128 caracteres
            if (line.length() >= 128) {
                result.append("Identificação do tipo de registro (campo 1 ao 3): ").append(line.substring(0, 3)).append("\n");
                result.append("Código da Fábrica Entrega (campo 4 ao 6): ").append(line.substring(3, 6)).append("\n");
                result.append("Identificação programa atual (campo 7 ao 15): ").append(line.substring(6, 15)).append("\n");
                result.append("Data do programa atual (campo 16 ao 21): ").append(line.substring(15, 21)).append("\n");
                result.append("Identificação programa anterior (campo 22 ao 30): ").append(line.substring(21, 30)).append("\n");
                result.append("Data do programa anterior (campo 31 ao 36): ").append(line.substring(30, 36)).append("\n");
                result.append("Código do item do cliente (campo 37 ao 66): ").append(line.substring(36, 66)).append("\n");
                result.append("Código do item do fornecedor (campo 67 ao 96): ").append(line.substring(66, 96)).append("\n");
                result.append("Número do pedido de compra (campo 97 ao 108): ").append(line.substring(96, 108)).append("\n");
                result.append("Código local do destino (campo 109 ao 113): ").append(line.substring(108, 113)).append("\n");
                result.append("Identificação para contato (campo 114 ao 124): ").append(line.substring(113, 124)).append("\n");
                result.append("Código unidade de medida (campo 125 ao 126): ").append(line.substring(124, 126)).append("\n");
                result.append("Quantidade de casas decimais (campo 127): ").append(line.substring(126, 127)).append("\n");
                result.append("Código tipo de fornecimento (campo 128): ").append(line.substring(127, 128)).append("\n");
            } else {
                result.append("Linha com tamanho insuficiente: ").append(line).append("\n");
            }
        }

        return result.toString();
    }


}