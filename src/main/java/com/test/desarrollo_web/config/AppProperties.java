package com.test.desarrollo_web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    private Upload upload = new Upload();
    private Validacion validacion = new Validacion();
    
    public Upload getUpload() {
        return upload;
    }
    
    public void setUpload(Upload upload) {
        this.upload = upload;
    }
    
    public Validacion getValidacion() {
        return validacion;
    }
    
    public void setValidacion(Validacion validacion) {
        this.validacion = validacion;
    }
    
    public static class Upload {
        private String dir;

        public String getDir() {
            return dir;
        }

        public void setDir(String dir) {
            this.dir = dir;
        }
    }
    
    public static class Validacion {
        private boolean habilitada;
        private String apiUrl;
        private String apiToken;

        public boolean isHabilitada() {
            return habilitada;
        }

        public void setHabilitada(boolean habilitada) {
            this.habilitada = habilitada;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getApiToken() {
            return apiToken;
        }

        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }
    }
}
