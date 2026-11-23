param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Args
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition

# Determine root project directory (handles running from root or from inside .venv\Scripts)
$ProjectRoot = $ScriptDir
if (Test-Path "$ScriptDir\..\..\src\compose_pilot" -ErrorAction SilentlyContinue) {
    $ProjectRoot = (Resolve-Path "$ScriptDir\..\..").Path
}

$env:PYTHONPATH = "$ProjectRoot\src;$ProjectRoot\.venv\Lib\site-packages;$env:PYTHONPATH"
$env:PYTHONIOENCODING = "utf-8"

$PythonExe = "python"
$candidate = "$env:LOCALAPPDATA\Python\bin\python.exe"
if (Test-Path $candidate -ErrorAction SilentlyContinue) {
    $PythonExe = $candidate
} else {
    $cmd = Get-Command python -ErrorAction SilentlyContinue
    if ($cmd) {
        $PythonExe = $cmd.Source
    }
}

& $PythonExe -m compose_pilot @Args
exit $LASTEXITCODE
