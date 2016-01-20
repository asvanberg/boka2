exports = this

person =
  controller: (args) ->
    @imgData = m.secureRequest(
      method: "GET"
      url: "/api/person/#{args.person.id()}/photo"
      deserialize: (a) -> a
      background: true
    )
    @imgData.then(m.redraw)
    return
  view: (ctrl, args) ->
    args ||= {}
    args.src = "data:image/jpeg;base64,#{ctrl.imgData()}" if ctrl.imgData()
    m "img", args

product =
  controller: (product) ->
    @imgData = m.prop null
    m.secureRequest(
      method: "GET"
      url: "/api/product/#{product().id()}/image"
    ).then(
      (image) =>
        @imgData("data:#{image.contentType};base64,#{image.data}")
    )
    return
  view: (ctrl, product) ->
    m "div.text-center",
      onclick: ->
        document.getElementById("product-#{product().id()}-image-upload").click()
      style:
        border: "3px dashed grey" unless ctrl.imgData()
      if ctrl.imgData()
        m "img",
          src: ctrl.imgData()
          style: "max-width": "100%"
      else
        m "span", "Click to upload image"
      m "input[type=file]",
        id: "product-#{product().id()}-image-upload"
        style:
          display: "none"
        onchange: (e) ->
          file = e.target.files[0]
          if (file)
            fr = new FileReader
            fr.addEventListener "load", (r) ->
              dataURL = r.target.result
              payload =
                data: dataURL.substring(dataURL.indexOf(",") + 1)
                contentType: file.type
                name: file.name
              m.secureRequest(
                method: "POST"
                url: "/api/product/#{product().id()}/image"
                data: payload
                deserialize: (d) -> d # No response, don't try to json parse it.
              ).then(ctrl.imgData.bind(this, dataURL))
            fr.readAsDataURL(file)

exports.components ||= {}
exports.components.image ||= {}
exports.components.image.person = person
exports.components.image.product = product
