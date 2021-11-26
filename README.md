pdnd-interop-commons
---

_Utility library for PDND interop_

### Specs

This implements some modules:

- `utils` - a module for common operations as type conversions;
- `file-manager` - a module for file management operations;
- `mail-manager` - a module for e-mail operations;
- `vault` - a module for read only accesses to Vault instances;
- `jwt` - a module for PDND tokens managemet;

---

The abovementioned modules are released as separated jar files, that you can import straightly and independently in any PDND component according to your needs.  

Probably each PDND component shall need at least the `utils` module, since it implements nitty common utility features.

Please mind that both `file-manager` and `mail-manager` depend on `utils` module, so importing any of these two in a component will automatically import `utils` as well.