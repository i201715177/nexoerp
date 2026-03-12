# Sistema para Farmacias y Boticas

Backend desarrollado en **Java 17** con **Spring Boot 3**.

## Estructura básica

- `pom.xml` – proyecto Maven Spring Boot.
- `src/main/java/com/farmacia/sistema/`  
  - `SistemaFarmaciaApplication.java` – clase principal.  
  - `config/SecurityConfig.java` – configuración de seguridad (todas las APIs abiertas para desarrollo).  
  - `api/ProductoController.java` – API REST para productos (`/api/productos`).  
  - `api/ClienteController.java` – API REST para clientes (`/api/clientes`).  
  - `api/VentaController.java` – API REST para ventas (`/api/ventas`).  
  - `api/venta/*` – DTOs para creación de ventas.  
  - `domain/producto/*` – entidad, repositorio y servicio de productos.  
  - `domain/cliente/*` – entidad, repositorio y servicio de clientes.  
  - `domain/venta/*` – entidad, repositorio y servicio de ventas.
- `src/main/resources/application.properties` – configuración (H2 en memoria para desarrollo).

## Cómo ejecutarlo (si tienes Java y Maven instalados)

```bash
mvn spring-boot:run
```

Luego podrás probar, por ejemplo:

- `GET http://localhost:8080/api/productos`
- `POST http://localhost:8080/api/productos`
- `GET http://localhost:8080/api/clientes`
- `POST http://localhost:8080/api/clientes`
- `GET http://localhost:8080/api/ventas`
- `POST http://localhost:8080/api/ventas`

Ejemplo de cuerpo para crear una venta:

```json
{
  "clienteId": 1,
  "items": [
    { "productoId": 1, "cantidad": 2 },
    { "productoId": 2, "cantidad": 1, "precioUnitario": 15.50 }
  ]
}
```

El modelo está pensado para ampliarse con módulos de recetas médicas, almacenes, usuarios/roles y reportes.

