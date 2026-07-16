package br.com.buni.integration.core.reader;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImportFileReader<T> {

    List<T> read(MultipartFile file);
}
