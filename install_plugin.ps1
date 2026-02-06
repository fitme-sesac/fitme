$url = "https://s3.amazonaws.com/session-manager-downloads/plugin/latest/windows/SessionManagerPluginSetup.exe"
$output = "$env:TEMP\SessionManagerPluginSetup.exe"

Write-Host "Downloading AWS Session Manager Plugin..."
Invoke-WebRequest -Uri $url -OutFile $output

Write-Host "Installing AWS Session Manager Plugin..."
Start-Process -FilePath $output -ArgumentList "/quiet" -Wait

Write-Host "Installation Complete."
Remove-Item $output
session-manager-plugin --version
