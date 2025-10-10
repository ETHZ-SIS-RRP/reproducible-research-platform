function Test-CommandExists
{
  param ($command)
  $oldPreference = $ErrorActionPreference
  $ErrorActionPreference = 'stop'
  try {if(Get-Command $command){return $true}}
  catch {return $false}
  finally {$ErrorActionPreference=$oldPreference}
}

if (!(Test-CommandExists docker)) {
  Write-Host "Command 'docker' was not found."
  Exit
}

if (-Not (docker ps -q -f name=rrp-player)) {
  if (-Not (Test-Path -Path $PSScriptRoot\openbis\.download-complete -PathType Leaf)) {
    $download_image = Get-Content $PSScriptRoot\download-image.txt -Raw
    docker run `
    -it `
    --rm `
    --pull always `
    --mount type=bind,source="$PSScriptRoot",target=/home/rrp/host `
    $download_image

    $success = $?
    if (-Not $success) {
      Write-Host "Downloading externally stored data was not successful"
      Exit
    }
  }

  New-Item -ItemType Directory -Force -Path $PSScriptRoot\results
  $image = Get-Content $PSScriptRoot\image.txt -Raw
  $command = Get-Content $PSScriptRoot\command.txt -Raw

  docker pull $image
  $success = $?
  if (-Not $success) {
    Write-Host "Pulling image failed"
    Exit
  }

  docker run `
  -d `
  --rm `
  --publish 8888:8888 `
  --mount "type=bind,source=$PSScriptRoot\repo,target=/home/jovyan/project" `
  --mount "type=bind,source=$PSScriptRoot\openbis,target=/home/jovyan/openbis" `
  --mount "type=bind,source=$PSScriptRoot\results,target=/home/jovyan/results" `
  --workdir /home/jovyan `
  --entrypoint /bin/bash `
  --name rrp-player `
  $image `
  -c "$command"

  Write-Host -NoNewline "Waiting for the container to start"
  while(-Not (docker exec -it rrp-player bash -c 'cat /proc/net/tcp 2>/dev/null|grep 22B8')) {
    Write-Host -NoNewline "."
    Start-Sleep -Seconds 2
  }
  Write-Host ""
}

Start-Process http://localhost:8888/lab
