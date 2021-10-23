package com.t1908e.memeportalapi;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

@SpringBootApplication
public class MemePortalApiApplication {
    public static void main(String[] args) throws IOException {
        ClassLoader classLoader = MemePortalApiApplication.class.getClassLoader();
        File file = new File(Objects.requireNonNull(Objects.requireNonNull(classLoader.getResource("serviceAccountKey.json")).getFile()));
        FileInputStream serviceAccount =
                new FileInputStream(file.getAbsolutePath());
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);
        SpringApplication.run(MemePortalApiApplication.class, args);
    }

    @Bean(name = "bCryptPasswordEncoder")
    public PasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
