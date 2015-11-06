m?.secureRequest ?= (options) ->
  oldConfig = options.config or () ->
  options.config = (xhr, opts) ->
    xhr.setRequestHeader("Authorization", "Bearer #{sessionStorage.getItem("id_token")}")
    oldConfig(xhr, opts)

  options.extract = (xhr) ->
    if xhr.status is 200 then xhr.responseText
    else "{\"status\": #{xhr.status}, \"body\": #{xhr.responseText or "\"\""}}"

  deferred = m.deferred()
  m.request(options)
    .then deferred.resolve, (d) ->
      if d.status is 401 then m.route "/login", return: m.route()
      else if d.status is 403 then m.route("/forbidden")
      else deferred.reject(d.body)
  deferred.promise