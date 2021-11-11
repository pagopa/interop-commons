pdnd-interop-commons
---

_Utility library for PDND interop_

### Specs

This implements three modules:

- `utils` - a module for common operations as type conversions;
- `file-manager` - a module for file management operations;
- `mail-manager` - a module for e-mail operations;

---

The abovementioned modules are released as separated jar files, that you can import straightly and independently in any PDND component according to your needs.  

Probably each PDND component shall need at least the `utils` module, since it implements nitty common utility features.

Please mind that both `file-manager` and `mail-manager` depend on `utils` module, so importing any of these two in a component will automatically import `utils` as well.

#### TODO

Currently this library misses of unit tests.