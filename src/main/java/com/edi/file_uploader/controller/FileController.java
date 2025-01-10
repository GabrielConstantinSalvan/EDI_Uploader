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
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;



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

    private List<Map<String, Object>> processContent(String content) {
        String[] lines = content.split("\\r?\\n"); // Divide o conteúdo em linhas
        List<Map<String, Object>> pedidos = new ArrayList<>();

       
        String dataCriacaoAtual = "";
        String numero_Pedido_De_Compra = "";
        String codigoAtual = "";
        int casasDecimais = 0;

        for (String line : lines) {
            line = line.trim();

            if (line.length() <= 128) { // Verifica se a linha tem o tamanho esperado
                if (line.startsWith("PE1")) {
                    // Extração de dados da linha PE1
                    
                    dataCriacaoAtual = formatDate(getFieldSafe(line, 15, 21)); // Data do programa atual
                    codigoAtual = getFieldSafe(line, 36, 66); // Código do item do cliente
                    numero_Pedido_De_Compra = getFieldSafe(line, 96, 108);
                    
                    String casasDecimaisStr = getFieldSafe(line, 126, 127); // Campo 127
                    try {
                        casasDecimais = Integer.parseInt(casasDecimaisStr != null ? casasDecimaisStr : "0");
                    } catch (NumberFormatException e) {
                        System.err.println("Erro ao converter casas decimais: " + casasDecimaisStr);
                        casasDecimais = 0; // Valor padrão
                    }
                } else if (line.startsWith("PE3")) {
                 Map<String, Object> pedido = new LinkedHashMap<>(); // Use LinkedHashMap para garantir a ordem
                 pedido.put("data_criacao", dataCriacaoAtual);
                 pedido.put("codigo", codigoAtual);
                 pedido.put("Numero_Pedido_De_Compra", numero_Pedido_De_Compra); // Adicione logo após "codigo"

                 List<Map<String, String>> itens = new ArrayList<>();
                 for (int i = 0; i < 6; i++) {
                     int startQuantidade = 11 + (17 * i);
                     int endQuantidade = startQuantidade + 9;
                     int startData = 3 + (17 * i);
                     int endData = startData + 6;

                     String quantidade = formatDecimal(getFieldSafe(line, startQuantidade, endQuantidade), casasDecimais);
                     String dataEntrega = formatDate(getFieldSafe(line, startData, endData));

                     Map<String, String> item = new HashMap<>();
                     item.put("quantidade", quantidade);
                     item.put("data_entrega", dataEntrega);

                     itens.add(item);
                 }
                 pedido.put("itens", itens);
                 pedidos.add(pedido);
             }


            } else {
                System.err.println("Linha ignorada por ser menor que 128 caracteres: " + line);
            }
        }
        return pedidos;
        
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Nenhum arquivo enviado.");
        }

        try {
            Path finalFile = Path.of(uploadDir + file.getOriginalFilename());
            Files.write(finalFile, file.getBytes());

            String content = new String(file.getBytes());
            List<Map<String, Object>> processedContent = processContent(content);

            return ResponseEntity.ok(processedContent); // Retorna como JSON
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao processar o arquivo: " + e.getMessage());
        }
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