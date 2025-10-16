shutdown /r /f /t 0
\\\above command will restart the server and closes all the pending process running on the server to make sure any new install to run smoothly without any errors.\\\
\\\Steps to create ISS file.we have to run this on power shell and get the install process completed to record the interaction clicks done during install process.\\\
cd D:\Builds_BIClientInstall\extract\BO4_3





$extractRoot = 'D:\Builds_BIClientInstall\extract\BO4_3'   
Get-ChildItem $extractRoot | Select Name,FullName,LastWriteTime
$setup = Get-ChildItem $extractRoot -Filter 'setup.exe' -Recurse -ErrorAction SilentlyContinue |
         Select-Object -First 1
if ($setup) {
  "Found setup: $($setup.FullName)"
} else {
  Write-Error "No setup.exe found under $extractRoot. Re-extract or check the path."
  return
}






$issPath = 'D:\Builds_BIClientInstall\BOClient\BO4_3.iss'
$null = New-Item -ItemType Directory -Force -Path (Split-Path $issPath)
Push-Location $setup.DirectoryName
$proc = Start-Process -FilePath $setup.FullName `
                      -ArgumentList @('/r', "/f1=$issPath") `
                      -PassThru
$proc.WaitForExit()
Pop-Location
if (Test-Path $issPath) {
  "Recorded: $issPath"
} else {
  Write-Error "Recording failed: $issPath not created. Re-run and click through all dialogs."
}
 
