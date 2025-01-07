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
        String[] lines = content.split("\\r?\\n"); // Divida o conteúdo por linhas
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            // Remover espaços extras antes de processar
            line = line.trim();

            // Verificar se a linha tem 128 caracteres
            if (line.length() <= 128) {
                if (line.startsWith("PE1")) {
                    // Processar PE1
                    result.append("---------------------------------COMEÇO PEDIDO---------------------------------\n");
                    result.append("TRADUÇÃO PE1\n");
                    result.append("----------------------------------------------------------------------------------\n");
                    result.append("Identificação do tipo de registro (campo 1 ao 3): ").append(line.substring(0, 3)).append("\n");
                    result.append("Código da Fábrica Entrega (campo 4 ao 6): ").append(getField(line, 3, 6)).append("\n");
                    result.append("Identificação programa atual (campo 7 ao 15): ").append(getField(line, 6, 15)).append("\n");
                    result.append("Data do programa atual (campo 16 ao 21): ").append(getField(line, 15, 21)).append("\n");
                    result.append("Identificação programa anterior (campo 22 ao 30): ").append(getField(line, 21, 30)).append("\n");
                    result.append("Data do programa anterior (campo 31 ao 36): ").append(getField(line, 30, 36)).append("\n");
                    result.append("Código do item do cliente (campo 37 ao 66): ").append(getField(line, 36, 66)).append("\n");
                    result.append("Código do item do fornecedor (campo 67 ao 96): ").append(getField(line, 66, 96)).append("\n");
                    result.append("Número do pedido de compra (campo 97 ao 108): ").append(getField(line, 96, 108)).append("\n");
                    result.append("Código local do destino (campo 109 ao 113): ").append(getField(line, 108, 113)).append("\n");
                    result.append("Identificação para contato (campo 114 ao 124): ").append(getField(line, 113, 124)).append("\n");
                    result.append("Código unidade de medida (campo 125 ao 126): ").append(getField(line, 124, 126)).append("\n");
                    result.append("Quantidade de casas decimais (campo 127): ").append(getField(line, 126, 127)).append("\n");
                    result.append("Código tipo de fornecimento (campo 128): ").append(getField(line, 127, 128)).append("\n");
                    result.append("----------------------------------------------------------------------------------\n");
                } else if (line.startsWith("PE2")) {
                    // Processar PE2
                    result.append("----------------------------------------------------------------------------------\n");
                    result.append("TRADUÇÃO PE2\n");
                    result.append("----------------------------------------------------------------------------------\n");
                    result.append("Identificação do tipo de Registro: ").append(line.substring(0, 3)).append("\n");
                    result.append("Data Da ultima entrega: ").append(getField(line, 3, 9)).append("\n");
                    result.append("Numero Ultima Nota Fiscal: ").append(getField(line, 9, 15)).append("\n");
                    result.append("Série Ultima Nota Fiscal: ").append(getField(line, 15, 19)).append("\n");
                    result.append("Data da ultima Nota fiscal: ").append(getField(line, 19, 25)).append("\n");
                    result.append("Quantidade da Ultima Entrega: ").append(getField(line, 25, 37)).append("\n");
                    result.append("Quantidade de entrega acumulada: ").append(getField(line, 37, 51)).append("\n");
                    result.append("Quantidade Necessária acumulada: ").append(getField(line, 51, 65)).append("\n");
                    result.append("Quantidade lote minimo: ").append(getField(line, 65, 77)).append("\n");
                    result.append("CÓDIGO FREQUENCIA DE FORNECIMENTO: ").append(getField(line, 77, 80)).append("\n");
                    result.append("DATA LIBERAÇÃO PARA PRODUÇÃO: ").append(getField(line, 80, 84)).append("\n");
                    result.append("DATA LIBERAÇÃO MATÉRIA PRIMA: ").append(getField(line, 84, 88)).append("\n");
                    result.append("CÓDIGO LOCAL DE DESCARGA: ").append(getField(line, 88, 95)).append("\n");
                    result.append("PERÍODO DE ENTREGA/EMBARQUE: ").append(getField(line, 95, 99)).append("\n");
                    result.append("CÓDIGO SITUAÇÃO DO ITEM: ").append(getField(line, 99, 101)).append("\n");
                    result.append("IDENTIFICAÇÃO DO TIPO DE PROGRAMAÇÃO: ").append(getField(line, 101, 102)).append("\n");
                    result.append("PERÍODO DE ENTREGA/EMBARQUE: ").append(getField(line, 102, 116)).append("\n");
                    result.append("PEDIDO DA REVENDA: ").append(getField(line, 116, 127)).append("\n");
                    result.append("QUALIFICAÇÃO DA PROGRAMAÇÃO: ").append(getField(line, 127, 128)).append("\n");
                    result.append("TIPO DE PEDIDO DA REVENDA: ").append(getField(line, 128, 130)).append("\n");
                    result.append("CÓDIGO DA VIA DE TRANSPORTE: ").append(getField(line, 130, 133)).append("\n");
                    result.append("ESPAÇOS: ").append(getField(line, 133, 256)).append("\n");
                    result.append("----------------------------------------------------------------------------------\n");
                } else  if (line.startsWith("PE3")) {
                    // Processar PE3
                    result.append("----------------------------------------------------------------------------------\n");
                    result.append("TRADUÇÃO PE3\n");
                    result.append("----------------------------------------------------------------------------------\n");
                    result.append("Identificação do tipo de Registro: ").append(line.substring(0, 3)).append("\n");
                    result.append("Data Entrega/Embarque do Item: ").append(getField(line, 3, 9)).append("\n");
                    result.append("Hora para Entrega/Embarque Item: ").append(getField(line, 9, 11)).append("\n");
                    result.append("Quantidade Entrega/Embarque do Item: ").append(getField(line, 11, 20)).append("\n");
                    result.append("Data Entrega/Embarque do Item (1): ").append(getField(line, 20, 26)).append("\n");
                    result.append("Hora para Entrega/Embarque Item (2): ").append(getField(line, 26, 28)).append("\n");
                    result.append("Quantidade Entrega/Embarque do Item (3): ").append(getField(line, 28, 37)).append("\n");
                    result.append("Data Entrega/Embarque do Item (2): ").append(getField(line, 36, 42)).append("\n");
                    result.append("Hora para Entrega/Embarque Item (3): ").append(getField(line, 42, 44)).append("\n");
                    result.append("Quantidade Entrega/Embarque do Item (4): ").append(getField(line, 44, 54)).append("\n");
                    result.append("Data Entrega/Embarque do Item (3): ").append(getField(line, 54, 60)).append("\n");
                    result.append("Hora para Entrega/Embarque Item (4): ").append(getField(line, 60, 62)).append("\n");
                    result.append("Quantidade Entrega/Embarque do Item (5): ").append(getField(line, 62, 71)).append("\n");
                    result.append("Data Entrega/Embarque do Item (4): ").append(getField(line, 71, 77)).append("\n");
                    result.append("Hora para Entrega/Embarque Item (5): ").append(getField(line, 77, 79)).append("\n");
                    result.append("Quantidade Entrega/Embarque do Item (6): ").append(getField(line, 79, 88)).append("\n");
                    result.append("Data Entrega/Embarque do Item (5): ").append(getField(line, 88, 94)).append("\n");
                    result.append("Hora para Entrega/Embarque Item (6): ").append(getField(line, 94, 96)).append("\n");
                    result.append("Quantidade Entrega/Embarque do Item (7): ").append(getField(line, 96, 105)).append("\n");
                    result.append("Data Entrega/Embarque do Item (6): ").append(getField(line, 105, 111)).append("\n");
                    result.append("Hora para Entrega/Embarque Item (7): ").append(getField(line, 111, 113)).append("\n");
                    result.append("Quantidade Entrega/Embarque do Item (8): ").append(getField(line, 113, 122)).append("\n");
                    result.append("Espaços: ").append(getField(line, 122, 128)).append("\n");
                    result.append("---------------------------------FIM DO PEDIDO---------------------------------\n");
                    result.append("-------------------------------------------------------------------------------\n");
                    
                    
                    String Pedido = getField(line, 97, 108);
                    String dataCriacao = getField(line, 31, 36);
                    String codigo = getField(line, 31, 36);
                    String quantidade = getField(line, 31, 36);
                    String dataEntrega = getField(line, 31, 36);
                    StringBuilder dadosExportacao = new StringBuilder();
                    
                    
                    dadosExportacao.append("Número do pedido de compra (campo 97 ao 108): ").append(Pedido).append("\n");
                    dadosExportacao.append("Data do programa anterior (campo 31 ao 36): ").append(dataCriacao).append("\n");
                    dadosExportacao.append("Código do item do cliente (campo 37 ao 66): ").append(codigo).append("\n");
                    dadosExportacao.append("Quantidade Entrega/Embarque do Item: ").append(quantidade).append("\n");
                    dadosExportacao.append("Data Entrega/Embarque do Item: ").append(dataEntrega).append("\n");
                    
                    
                }
            } else {
                result.append("Linha ignorada por não ter 128 caracteres: ").append(line).append("\n");
            }
        }

        return result.toString();
    }
    


    // Método auxiliar para preencher espaços em branco
    private String getField(String line, int start, int end) {
        String field = line.length() >= end ? line.substring(start, end) : "";
        return field + " ".repeat(end - start - field.length());
    }
}
