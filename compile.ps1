$files = Get-ChildItem -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
if ($files.Count -gt 0) {
    javac -encoding UTF-8 -d out $files
}
