pipeline {
  agent any

  parameters {
    string(name: 'TARGET_SERVERS', defaultValue: '172.26.130.208', description: 'Comma-separated list of target servers.')
    string(name: 'SHARE_FILE', defaultValue: '\\\\1062589-DANSQA\\DevSource\\BI_Tools\\BOSDK4_3', description: 'UNC path to shared files.')
    string(name: 'DEST_DIR', defaultValue: 'D:\\Builds_BOSDKInstall', description: 'Destination path on remote server.')
    string(name: 'USER', defaultValue: 'starbase\\teamcitysrvacct', description: 'Domain\\user for remote operations.')
    password(name: 'Password', defaultValue: '', description: 'Password for the user.')
    string(name: 'INSTALL_ARGS', defaultValue: '/quiet /norestart', description: 'Install arguments for setup.exe (not used here).')
  }

  stages {
    stage('Create Folder on Remote Servers') {
      steps {
        script {
          def servers = params.TARGET_SERVERS.split(/\s*,\s*/).findAll { it?.trim() }
          if (servers.isEmpty()) error 'No TARGET_SERVERS provided.'

          def safePassword = params.Password.getPlainText().replace("'", "''")

          for (server in servers) {
            def psScript = """
              \$secpasswd = ConvertTo-SecureString '${safePassword}' -AsPlainText -Force
              \$cred = New-Object System.Management.Automation.PSCredential ('${params.USER}', \$secpasswd)
              Invoke-Command -ComputerName '${server}' -Credential \$cred -ScriptBlock {
                param(\$folder)
                if (-Not (Test-Path -Path \$folder)) {
                  New-Item -Path \$folder -ItemType Directory | Out-Null
                  Write-Host "✅ Folder created: \$folder"
                } else {
                  Write-Host "📁 Folder already exists: \$folder"
                }
              } -ArgumentList '${params.DEST_DIR}'
            """

            def psScriptFile = "createFolder_${server}.ps1"
            writeFile file: psScriptFile, text: psScript
            bat "powershell.exe -NoProfile -ExecutionPolicy Bypass -File ${psScriptFile}"
            bat "del ${psScriptFile}"
          }
        }
      }
    }

    // ✅ Add this stage
    stage('Copy Files to Remote Servers') {
      steps {
        script {
          def servers = params.TARGET_SERVERS.split(/\s*,\s*/).findAll { it?.trim() }
          def safePassword = params.Password.getPlainText().replace("'", "''")

          for (server in servers) {
            def psScript = """
              \$secpasswd = ConvertTo-SecureString '${safePassword}' -AsPlainText -Force
              \$cred = New-Object System.Management.Automation.PSCredential ('${params.USER}', \$secpasswd)

              Invoke-Command -ComputerName '${server}' -Credential \$cred -ScriptBlock {
                \$share = '${params.SHARE_FILE}'
                \$dest = '${params.DEST_DIR}'

                net use \$share /user:${params.USER} '${safePassword}' > \$null

                if (!(Test-Path -Path \$dest)) {
                  New-Item -Path \$dest -ItemType Directory | Out-Null
                }

                Write-Host "Copying files from \$share to \$dest..."
                \$result = robocopy \$share \$dest /E /Z /R:2 /W:5
                if (\$LASTEXITCODE -ge 8) {
                  throw "Robocopy failed with exit code \$LASTEXITCODE"
                }

                net use \$share /delete > \$null
              }
            """
            def psScriptFile = "copyFiles_${server}.ps1"
            writeFile file: psScriptFile, text: psScript
            bat "powershell.exe -NoProfile -ExecutionPolicy Bypass -File ${psScriptFile}"
            bat "del ${psScriptFile}"
          }
        }
      }
    }
  }
}
