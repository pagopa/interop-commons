interop-commons
---

_Utility library for interop_

### Specs

This implements some modules:

- `utils` - a module for common operations as type conversions;
- `file-manager` - a module for file management operations;
- `mail-manager` - a module for e-mail operations;
- `signer` - a module for read only accesses to AWS KMS;
- `jwt` - a module for Interop tokens management;

---

The abovementioned modules are released as separated jar files, that you can import straightly and independently in any Interop component according to your needs.

Probably each Interop component shall need at least the `utils` module, since it implements nitty common utility features.

### Utility and Logging Module
This is the HOCON configuration object for the module:

```
interop-commons {
  isInternetFacing = true
}
```

Where:

| Variable name                  | Variable type | Notes                           |
|--------------------------------|---------------|---------------------------------|
| **isInternetFacing**           | Boolean       | Used to apply security policies |

### File Manager Module
This is the HOCON configuration object for the module:

```
interop-commons {
  storage {
    type = ${STORAGE_TYPE}
    endpoint = ${STORAGE_ENDPOINT}
    application {
      id = ${STORAGE_APPLICATION_ID}
      secret = ${STORAGE_APPLICATION_SECRET}
    }
  }
}
```

Where:

| Variable name                  | Variable type | Notes                                                                            |
|--------------------------------| ------------- |----------------------------------------------------------------------------------|
| **STORAGE_TYPE**               | String | Admittable values are: `File`, `S3`, `BlobStorage`                               |
| **STORAGE_ENDPOINT**           | String | Defines the remote endpoint to connect to                                        |
| **STORAGE_APPLICATION_ID**     | String | Defines the user credential to access the remote endpoint (not required for AWS) |
| **STORAGE_APPLICATION_SECRET** | String | Defines the user password to access the remote endpoint (not required for AWS)                         |

### Mail Manager Module
This is the HOCON configuration object for the module:

```
mailer {
  sender = ${MAIL_SENDER_ADDRESS}
  smtp {
    user = ${SMTP_USR}
    password = ${SMTP_PSW}
    server-address = ${SMTP_SERVER}
    server-port = ${SMTP_PORT}
    authenticated = ${SMTP_AUTHENTICATED}
    with-tls = ${SMTP_WITH_TLS}
    with-ssl = ${SMTP_WITH_SSL}
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
| **SMTP_WITH_SSL**       | Boolean, default true | Flag stating if the mailer MUST work through SSL            |

### AWS KMS Module
This is the HOCON configuration object for the module:

```
interop-commons {
  max-concurrency = 50
}
```

Where:

| Variable name                  | Variable type | Notes                                  |
|--------------------------------|---------------|----------------------------------------|
| **max-concurrency**            | Integer       | Used to apply max concurrency threads  |

### JWT Module
This is the HOCON configuration object for the module:

```
interop-commons {
  jwt {
    public-keys {
      urls = ${WELL_KNOWN_URLS}
      size-limit = ${SIZE_LIMIT}
      connection-timeout = ${CONNECTION_TIMEOUT}
      read-timeout = ${READ_TIMEOUT}
    }

    internal-token {
      issuer = ${JWT_ISSUER}
      subject = ${JWT_SUBJECT}
      audience = ${JWT_AUDIENCE}
      duration-seconds = ${JWT_DURATION_SECONDS}
    }
  }
}
```

Where:

| Variable name            | Variable type      | Notes                                                                                        |
|--------------------------|--------------------|----------------------------------------------------------------------------------------------|
| **WELL_KNOWN_URL**       | String             | URL address of the Well Known exposing the public JWK set.                                   |
| **SIZE_LIMIT**           | Integer, default 0 | The read size limit, in bytes. If zero no limit.                                             |
| **CONNECTION_TIMEOUT**   | Integer, default 0 | The URL connection timeout, in milliseconds. If zero no (infinite) timeout.                  |
| **READ_TIMEOUT**         | Integer, default 0 | The URL read timeout, in milliseconds. If zero no (infinite) timeout.                        |
| **JWT_ISSUER**           | String             | The issuer of the internal interop token                                                     |
| **JWT_SUBJECT**          | String             | The subject of the internal interop token                                                    |
| **JWT_AUDIENCE**         | String             | The only audience of the internal token |
| **JWT_DURATION_SECONDS** | Long               | The token validity in seconds                                 |


### AWS
AWS credentials are implicitly derived by the SDK as per [AWS documentation](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html)  
:warning: When running locally it is possible that the library will use default credentials in your `~/.aws/credentials` file. To avoid unwanted behaviours, remember to set AWS credentials environment variables
