m?.secureRequest ?= (options) ->
  oldConfig = options.config or () ->
  options.config = (xhr, opts) ->
    xhr.setRequestHeader("Authorization", "Bearer #{sessionStorage.getItem("id_token")}")
    oldConfig(xhr, opts)

  options.extract = (xhr) ->
    if xhr.status is 200 then xhr.responseText
    else xhr.status

  deferred = m.deferred()
  m.request(options)
    .then deferred.resolve, (status) ->
      if status is 401 then m.route("/login")
      else if status is 403 then m.route("/forbidden")
      else deferred.reject(status)
  deferred.promise