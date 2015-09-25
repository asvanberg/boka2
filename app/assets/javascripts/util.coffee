m?.secureRequest ?= (options) ->
  oldConfig = options.config || () ->
  options.config = (xhr, opts) ->
    xhr.setRequestHeader("Authorization", "Bearer #{sessionStorage.getItem("id_token")}")
    oldConfig(xhr, opts)

  options.extract = (xhr) ->
    console.log("(xtract")
    if xhr.status == 200 then xhr.responseText
    else xhr.status

  deferred = m.deferred()
  m.request(options)
    .then deferred.resolve, (status) ->
      console.log(status)
      if status == 401 then m.route("/login")
      else if status == 403 then m.route("/forbidden")
      else deferred.reject(status)
  deferred.promise