exports = this

person =
  controller: (args) ->
    @imgData = m.secureRequest(
      method: "GET"
      url: "/admin/person.photo?id=#{args.person.id()}"
      deserialize: (a) -> a
      background: true
    )
    @imgData.then(m.redraw)
    return
  view: (ctrl, args) ->
    args ||= {}
    args.src = "data:image/jpeg;base64,#{ctrl.imgData()}" if ctrl.imgData()
    m "img", args

exports.components ||= {}
exports.components.image ||= {}
exports.components.image.person = person