exports = this

class Modal
  constructor: ->
    @shown = m.prop false

  show: ->
    @shown true
    document.body.classList.add "modal-open"

  hide: ->
    @shown false
    document.body.classList.remove "modal-open"

  view: (title, args) ->
    if @shown()
      m "div", [
        m ".modal.in",
          style:
            display: "block"
          onclick: @hide.bind this
          m ".modal-dialog", {onclick: (e) -> e.stopPropagation()}, [
            m ".modal-content", [
              m ".modal-header", [
                m "button.close", {onclick: @hide.bind this}, m "span", "×"
                m "h4.modal-title", title()
              ]
              m ".modal-body", args.body()
              m ".modal-footer", args.footer() if args.footer
            ]
          ]
        m ".modal-backdrop.in"
      ]

exports.Modal = Modal