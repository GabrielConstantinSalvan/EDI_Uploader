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
import java.util.ArrayList;
import java.util.List;


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
        String[] lines = content.split("\\r?\\n"); // Divide o conteúdo por linhas
        List<String[]> tabela = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();

            if (line.length() <= 128) {
                if (line.startsWith("PE1")) {
                    String pedido = getField(line, 96, 108).trim();
                    String dataCriacao = getField(line, 30, 36).trim();
                    String codigo = getField(line, 36, 66).trim();

                    tabela.add(new String[]{pedido, dataCriacao, codigo, "", ""}); // Quantidade e Data Entrega vazias
                } else if (line.startsWith("PE3")) {
                    String quantidade = getField(line, 11, 20).trim();
                    String dataEntrega = getField(line, 3, 9).trim();

                    if (!tabela.isEmpty()) {
                        String[] ultimaLinha = tabela.get(tabela.size() - 1);
                        tabela.add(new String[]{ultimaLinha[0], ultimaLinha[1], ultimaLinha[2], quantidade, dataEntrega});
                    }
                }
            }
        }

        // Calcular larguras de colunas
        int[] colWidths = { "Pedido".length(), "Data Criação".length(), "Código".length(), "Quantidade".length(), "Data de entrega".length() };

        for (String[] linha : tabela) {
            for (int i = 0; i < linha.length; i++) {
                colWidths[i] = Math.max(colWidths[i], linha[i].length());
            }
        }

        // Construir a tabela formatada
        StringBuilder result = new StringBuilder();
        result.append(formatLine(new String[]{"Pedido", "Data Criação", "Código", "Quantidade", "Data de entrega"}, colWidths));
        result.append(formatLine(new String[]{"-" + "-".repeat(colWidths[0] - 2), "-" + "-".repeat(colWidths[1] - 2), "-" + "-".repeat(colWidths[2] - 2), "-" + "-".repeat(colWidths[3] - 2), "-" + "-".repeat(colWidths[4] - 2)}, colWidths));

        for (String[] linha : tabela) {
            result.append(formatLine(linha, colWidths));
        }

        return result.toString();
    }

    private String formatLine(String[] values, int[] colWidths) {
        StringBuilder line = new StringBuilder("|");
        for (int i = 0; i < values.length; i++) {
            line.append(" ").append(padRight(values[i], colWidths[i])).append(" |");
        }
        line.append("\n");
        return line.toString();
    }

    private String padRight(String text, int length) {
        return text + " ".repeat(Math.max(0, length - text.length()));
    }


    


    // Método auxiliar para preencher espaços em branco
    private String getField(String line, int start, int end) {
        String field = line.length() >= end ? line.substring(start, end) : "";
        return field + " ".repeat(end - start - field.length());
    }
}
