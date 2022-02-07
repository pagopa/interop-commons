pdnd-interop-commons
---

_Utility library for PDND interop_

### Specs

This implements some modules:

- `utils` - a module for common operations as type conversions;
- `file-manager` - a module for file management operations;
- `mail-manager` - a module for e-mail operations;
- `vault` - a module for read only accesses to Vault instances;
- `jwt` - a module for PDND tokens management;

---

The abovementioned modules are released as separated jar files, that you can import straightly and independently in any PDND component according to your needs.  

Probably each PDND component shall need at least the `utils` module, since it implements nitty common utility features.

### File Manager Module
This is the HOCON configuration object for the module:

```
pdnd-interop-commons {
  storage {
    type = ${STORAGE_TYPE}
    endpoint = ${STORAGE_ENDPOINT}
    path = ${STORAGE_PATH}
    application {
      id = ${STORAGE_APPLICATION_ID}
      secret = ${STORAGE_APPLICATION_SECRET}
    }
  }
}
```

Where:

| Variable name                  | Variable type | Notes                                                                    |
|--------------------------------| ------------- |--------------------------------------------------------------------------|
| **STORAGE_TYPE**               | String | Admittable values are: `File`, `S3`, `BlobStorage`                       |
| **STORAGE_CONTAINER**          | String | Defines the container holding the data (e.g.: S3 bucket name)            |
| **STORAGE_PATH**               | String | Defines the path holding the data within the container (e.g.: S3 prefix) |
| **STORAGE_ENDPOINT**           | String | Defines the remote endpoint to connect to                                |
| **STORAGE_APPLICATION_ID**     | String | Defines the user credential to access the remote endpoint                |
| **STORAGE_APPLICATION_SECRET** | String | Defines the user password to access the remote endpoint                  |

### Mail Manager Module
This is the HOCON configuration object for the module:

```
 pdnd-interop-commons {
   mail {
     sender = ${MAIL_SENDER_ADDRESS}
     smtp {
       user = ${SMTP_USR}
       password = ${SMTP_PSW}
       server = ${SMTP_SERVER}
       port = ${SMTP_PORT}
       authenticated = ${SMTP_AUTHENTICATED}
       with-tls = ${SMTP_WITH_TLS}
     }
   }
 }
```
Where:

| Variable name           | Variable type         | Notes                                                       |
|-------------------------|-----------------------|-------------------------------------------------------------|
| **MAIL_SENDER_ADDRESS** | String                | Component mail sender address, e.g.: pagopa-interop@test.me |
| **SMTP_USR**            | String                | SMTP username                                               |
| **SMTP_PSW**            | String                | SMTP user password                                          |
| **SMTP_SERVER**         | String                | SMTP server address                                         |
| **SMTP_PORT**           | Integer               | SMTP server port                                            |
| **SMTP_AUTHENTICATED**  | Boolean, default true | Flag stating if this mailer must be authenticated           |
| **SMTP_WITH_TLS**       | Boolean, default true | Flag stating if the mailer MUST work through TLS            |

### Vault Module
This is the HOCON configuration object for the module:

```
pdnd-interop-commons {
  vault {
    address = ${VAULT_ADDR}
    token = ${VAULT_TOKEN}
    sslEnabled = ${VAULT_SSL_ENABLED}
    }
  }
```

Where:

| Variable name           | Variable type         | Notes                                         |
|-------------------------|-----------------------|-----------------------------------------------|
| **VAULT_ADDR** | String                | URL address of the Vault                      |
| **VAULT_TOKEN**            | String                | Token for accessing the Vault                 |
| **VAULT_SSL_ENABLED**            | Boolean, default true | Flag stating if the Vault client MUST use SSL |

### JWT Module
This is the HOCON configuration object for the module:

```
pdnd-interop-commons {
  jwt {
    public-keys {
      url = ${WELL_KNOWN_URL}
      size-limit = ${SIZE_LIMIT}
      connection-timeout = ${CONNECTION_TIMEOUT}
      read-timeout = ${READ_TIMEOUT}
    }
  }
}
```

Where:

| Variable name           | Variable type      | Notes                                                      |
|-------------------------|--------------------|------------------------------------------------------------|
| **WELL_KNOWN_URL** | String             | URL address of the Well Known exposing the public JWK set. |
| **SIZE_LIMIT**            | Integer, default 0 | The read size limit, in bytes. If zero no limit.              |
| **CONNECTION_TIMEOUT**            | Integer, default 0 | The URL connection timeout, in milliseconds. If zero no (infinite) timeout.              |
| **READ_TIMEOUT**            | Integer, default 0 | The URL read timeout, in milliseconds. If zero no (infinite) timeout.              |
