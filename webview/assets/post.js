if (Android) {
	Android.log("[JS] Form submit override")
	for (let form of document.querySelectorAll("form")) {
		Android.log(`[JS] Override form: ${form}`)
		form.onsubmit = (e) => {
			e.preventDefault()
			Android.log(`Submit on form: ${form}`)
			const data = new FormData(form)
			let obj = {}
			data.forEach((v, k) => obj[k] = v)
			path = window.location.pathname.split("/").pop().replace(".html", "")
			json = JSON.stringify(obj)
			Android.log(`[JS] Sending to ${path}: '${json}'`)
			Android.post(path, json)
			return false
		}
	}
}

