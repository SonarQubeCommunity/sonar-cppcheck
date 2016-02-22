Descriptions of rules from different Cppcheck versions for automatic merge.

# Export rules using the bash script

Use ```export-rules.sh``` to export the rules, for example:

```bash
./export-rules.sh 1.68 1.69 1.70
```

# Export rules manually

- Download the source of the new cppcheck version from http://sourceforge.net/projects/cppcheck/files/cppcheck/
- Uncompress and build cppcheck using the command:

```bash
make
```

- Export the rules using ```cppcheck```, for example:

```bash
./cppcheck --xml --xml-version=2 --errorlist > cppcheck-1.68.xml
```
