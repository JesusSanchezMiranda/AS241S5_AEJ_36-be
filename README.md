# AI Image Services con Spring Boot WebFlux

**Servicio reactivo** que consume dos APIs de Inteligencia Artificial de RapidAPI: **Colorize Photo** (colorización de imágenes en blanco y negro) y **AI Background Remover** (eliminación de fondo de imágenes). Los resultados se almacenan en PostgreSQL alojado en Neon Cloud usando R2DBC reactivo.

---

## **1. APIs de Inteligencia Artificial (RapidAPI)**

### **1.1 Colorize Photo**
- **Plataforma:** RapidAPI — Colorize Photo
- **Endpoint:** `POST /colorize_image_with_auto_prompt`
- **Host:** `colorize-photo1.p.rapidapi.com`
- **Entrada:** Imagen como archivo `multipart/form-data`
- **Salida:** Imagen JPEG colorizada (bytes binarios)

### **1.2 AI Background Remover**
- **Plataforma:** RapidAPI — AI Background Remover
- **Endpoint:** `POST /image/matte/v1`
- **Host:** `ai-background-remover.p.rapidapi.com`
- **Entrada:** Imagen como archivo `multipart/form-data`
- **Salida:** Imagen PNG sin fondo (bytes binarios)

---

## **2. Stack Tecnológico**

<img src="https://miro.medium.com/v2/resize:fit:716/1*98O4Gb5HLSlmdUkKg1DP1Q.png" align="right" style="height:60px; width: 200px"/>

- **Java:** JDK 17
- **IDE:** IntelliJ IDEA
- **Build:** Apache Maven
- **Framework:** Spring Boot 3.x
- **Reactivo:** Spring WebFlux + Project Reactor
- **BD Cloud:** PostgreSQL en Neon.tech (Free Tier)
- **Driver reactivo:** R2DBC PostgreSQL


---

## **3. Dependencias Maven**

### **Spring WebFlux + PostgreSQL (SQL Reactivo)**
Spring WebFlux | Spring Data R2DBC | Project Reactor | R2DBC PostgreSQL | Jackson
```xml

    org.springframework.boot
    spring-boot-starter-webflux


    org.springframework.boot
    spring-boot-starter-data-r2dbc


    org.postgresql
    r2dbc-postgresql
    runtime


    org.projectlombok
    lombok
    true


    io.projectreactor
    reactor-test
    test


    com.fasterxml.jackson.core
    jackson-databind

```





## **4. Endpoints REST**

| Método | URL | Descripción |
|--------|-----|-------------|
| `POST` | `/api/images/colorize` | Coloriza imagen en blanco y negro |
| `POST` | `/api/images/remove-bg` | Elimina fondo de imagen |
| `GET`  | `/api/images/output/{filename}` | Ver imagen procesada |
| `GET`  | `/api/images/results` | Todos los resultados en BD |
| `GET`  | `/api/images/results/{apiName}` | Filtrar por `colorize` o `remove-bg` |

---

## **5. Schema de Base de Datos**

Tabla `ai_image_results` — almacena metadata de cada operación realizada:
```sql
CREATE TABLE IF NOT EXISTS ai_image_results (
    id          BIGSERIAL PRIMARY KEY,
    api_name    VARCHAR(50)  NOT NULL,
    input_url   TEXT         NOT NULL,
    output_url  TEXT,
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    DEFAULT NOW()
);
```

---

## **6. Cómo Probar con Postman**

### Colorizar imagen
```
POST http://localhost:8082/api/images/colorize
Body: form-data → Key: file (tipo File) → imagen .jpg en blanco y negro
```

### Remover fondo
```
POST http://localhost:8082/api/images/remove-bg
Body: form-data → Key: file (tipo File) → imagen .jpg o .png
```

### Ver imagen procesada
```
GET http://localhost:8082/api/images/output/{filename}
```
> Abre la URL directo en el navegador para ver la imagen.

### Consultar historial en BD
```
GET http://localhost:8082/api/images/results
GET http://localhost:8082/api/images/results/colorize
GET http://localhost:8082/api/images/results/remove-bg
```