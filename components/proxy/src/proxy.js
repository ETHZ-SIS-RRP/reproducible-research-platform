const http = require('http')
const httpProxy = require('http-proxy')
const CryptoJS = require('crypto-js')
const fs = require("fs");

const proxy = httpProxy.createProxyServer({})

const secret = fs.readFileSync("/rrp-secrets/cookie-secret").toString();

const key = CryptoJS.PBKDF2(secret, "salt!", {
  keySize: 256 / 32,
  iterations: 65536
})

function trimLeft(string, charToRemove) {
  var res = string
  while (res.charAt(0) == charToRemove) {
    res = res.substring(1)
  }
  return res
}

function decrypt(base64CipherText) {
  return CryptoJS.AES.decrypt(
    {
      ciphertext: CryptoJS.enc.Base64.parse(base64CipherText),
      salt: ""
    },
    key,
    {
      mode: CryptoJS.mode.ECB,
      padding: CryptoJS.pad.Pkcs7
    }
  ).toString(CryptoJS.enc.Utf8)
}

function readUserName(request) {
  let username = "unknown"
  try {
    const index = request.rawHeaders.findIndex(element => element.toLowerCase() === 'cookie')
    if (index !== -1) {
      const allCookies = request.rawHeaders[index + 1].split(";").map(value => value.trim())
      const cookie = allCookies.find(element => element.startsWith('rrp-session='))
      if (cookie !== null) {
        data = cookie.substring(12)
        const plaintext = decrypt(data)
        const session = JSON.parse(plaintext)
        username = session.username
      }
    }
  } catch (error) {
  }
  return username
}

function getDirectories(source) {
  return fs.readdirSync(source, { withFileTypes: true })
    .filter(dirent => dirent.isDirectory())
    .map(dirent => dirent.name)
}

function getTarget(req) {
  let server = "unknown"
  let matlab = false
  try {
    const username = readUserName(req)
    const original = req.url
    const parts = trimLeft(original, '/').split('/')
    const serverName = parts[1]
    const uid = serverName.split('-')[0]
    const projects = getDirectories(process.env.rrp_proxy_home_dir + "/" + username + "/projects")
    if (projects.some(e => e === uid)) {
      server = serverName + '-' + username
    }

    if (parts.at(2) == "matlab") {
      matlab = true
    }

  } catch (error) {
    console.log(error)
  }

  const target = "http://server-" + server + ".analysis-server." + process.env.rrp_namespace + ".svc.cluster.local:" + (matlab ? "8889" : "8888")
  return target
}

var server = http.createServer(function (req, res) {
  proxy.web(req, res, {
    target: getTarget(req)
  }, function (err) {
    console.log(req.url + ": 404")
    res.writeHead(404)
    return res.end()
  })
})

server.on('upgrade', function (req, socket, head) {
  proxy.ws(req, socket, head, {
    target: getTarget(req)
  }, function (err) {
    console.log(req.url + " (websocket): 404")
    return socket.destroy()
  })
})

console.log("listening on port 8080, in namespace " + process.env.rrp_namespace)
server.listen(8080)
