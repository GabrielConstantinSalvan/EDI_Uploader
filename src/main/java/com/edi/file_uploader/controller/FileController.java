package com.edi.file_uploader.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
        String[] lines = content.split("\\r?\\n"); // Divide o conteúdo em linhas
        List<String[]> tabela = new ArrayList<>();

        // Variáveis temporárias para armazenar dados do EDI
        String pedidoAtual = "";
        String dataCriacaoAtual = "";
        String codigoAtual = "";
        int casasDecimais = 0; // Armazena a quantidade de casas decimais

        for (String line : lines) {
            line = line.trim();

            if (line.length() <= 128) { // Verifica se a linha tem o tamanho esperado
                if (line.startsWith("PE1")) {
                    // Extração de dados da linha PE1
                    pedidoAtual = getFieldSafe(line, 96, 108); // Número do pedido de compra
                    dataCriacaoAtual = getFieldSafe(line, 15, 21); // Data do programa atual
                    codigoAtual = getFieldSafe(line, 36, 66); // Código do item do cliente

                    // Extração do campo 127 (quantidade de casas decimais)
                    String casasDecimaisStr = getFieldSafe(line, 126, 127); // Campo 127
                    try {
                        casasDecimais = Integer.parseInt(casasDecimaisStr != null ? casasDecimaisStr : "0");
                    } catch (NumberFormatException e) {
                        System.err.println("Erro ao converter casas decimais: " + casasDecimaisStr);
                        casasDecimais = 0; // Valor padrão
                    }
                } else if (line.startsWith("PE3")) {
                    // Extração de dados da linha PE3
                    String quantidade1 = formatDecimal(getFieldSafe(line, 28, 37), casasDecimais);
                    String dataEntrega1 = formatDate(getFieldSafe(line, 20, 26));

                    String quantidade2 = formatDecimal(getFieldSafe(line, 45, 54), casasDecimais);
                    String dataEntrega2 = formatDate(getFieldSafe(line, 37, 43));

                    String quantidade3 = formatDecimal(getFieldSafe(line, 62, 71), casasDecimais);
                    String dataEntrega3 = formatDate(getFieldSafe(line, 54, 60));

                    String quantidade4 = formatDecimal(getFieldSafe(line, 79, 88), casasDecimais);
                    String dataEntrega4 = formatDate(getFieldSafe(line, 71, 77));

                    String quantidade5 = formatDecimal(getFieldSafe(line, 96, 105), casasDecimais);
                    String dataEntrega5 = formatDate(getFieldSafe(line, 88, 94));

                    String quantidade6 = formatDecimal(getFieldSafe(line, 113, 122), casasDecimais);
                    String dataEntrega6 = formatDate(getFieldSafe(line, 105, 111));

                    // Adiciona os dados extraídos à tabela
                    tabela.add(new String[]{pedidoAtual, dataCriacaoAtual, codigoAtual, quantidade1, dataEntrega1, quantidade2, dataEntrega2, quantidade3, dataEntrega3, quantidade4, dataEntrega4, quantidade5, dataEntrega5, quantidade6, dataEntrega6});
                }
            } else {
                System.err.println("Linha ignorada por ser menor que 128 caracteres: " + line);
            }
        }

        // Construção da tabela formatada
        return buildTable(tabela);
    }

    // Método auxiliar para formatar um valor como decimal, levando em conta o número de casas decimais
    private String formatDecimal(String value, int casasDecimais) {
        try {
            if (value != null && !value.isEmpty()) {
                BigDecimal decimalValue = new BigDecimal(value.trim());
                return decimalValue.setScale(casasDecimais, RoundingMode.HALF_UP).toString();
            } else {
                return casasDecimais == 0 ? "0" : "0.00";
            }
        } catch (NumberFormatException e) {
            System.err.println("Erro ao converter para decimal: " + value);
            return casasDecimais == 0 ? "0" : "0.00";
        }
    }



 // Método auxiliar para formatar datas no formato dd-mm-aa
    private String formatDate(String value) {
        if (value == null || value.isEmpty() || value.equals("000000")) {
            return "N/A";
        }

        try {
            // Verifica se o tamanho do campo é válido para uma data
            if (value.length() == 6) {
                String ano = value.substring(0, 2);
                String mes = value.substring(2, 4);
                String dia = value.substring(4, 6);
                return dia + "-" + mes + "-" + ano;
            } else {
                System.err.println("Formato de data inválido: " + value);
                return "N/A";
            }
        } catch (Exception e) {
            System.err.println("Erro ao formatar data: " + value + " - " + e.getMessage());
            return "N/A";
        }
    }






    private String buildTable(List<String[]> tabela) {
        StringBuilder builder = new StringBuilder();

        // Cabeçalho da tabela
        builder.append("| Pedido     | Data Criação | Código         | Quantidade 1  |Data de entrega 1| Quantidade 2   | Data de entrega 2 | Quantidade 3   | Data de entrega 3 | Quantidade 4   | Data de entrega 4 | Quantidade 5   | Data de entrega 5 | Quantidade 6   | Data de entrega 6 |\n");
        builder.append("|------------|--------------|----------------|---------------|-----------------|----------------|-------------------|----------------|-------------------|----------------|-------------------|----------------|-------------------|----------------|-------------------|\n");

        // Linhas da tabela
        for (String[] row : tabela) {
            builder.append(String.format("| %-10s | %-12s | %-14s | %-13s | %-15s | %-14s | %-17s | %-14s | %-17s | %-14s | %-17s | %-14s | %-17s | %-14s | %-17s |\n",
                row[0] != null ? row[0] : "N/A",
                row[1] != null ? row[1] : "N/A",
                row[2] != null ? row[2] : "N/A",
                row[3] != null ? row[3] : "N/A",
                row[4] != null ? row[4] : "N/A",
                row[5] != null ? row[5] : "N/A",
                row[6] != null ? row[6] : "N/A",
                row[7] != null ? row[7] : "N/A",
                row[8] != null ? row[8] : "N/A",
                row[9] != null ? row[9] : "N/A",
                row[10] != null ? row[10] : "N/A",
                row[11] != null ? row[11] : "N/A",
                row[12] != null ? row[12] : "N/A",
                row[13] != null ? row[13] : "N/A",
                row[14] != null ? row[14] : "N/A"));
        }

        return builder.toString();
    }






    // Método auxiliar para preencher espaços em branco
    private String getFieldSafe(String line, int start, int end) {
        try {
            if (line.length() >= end) {
                return line.substring(start, end).trim();
            } else {
                System.err.println("Campo fora do limite na linha: " + line);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Erro ao extrair campo: " + e.getMessage());
            return null;
        }
    }



}