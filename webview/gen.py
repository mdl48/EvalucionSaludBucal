from jinja2 import Environment, FileSystemLoader
from os.path import dirname, abspath

dir = dirname(abspath(__file__))
jinja = Environment(loader=FileSystemLoader(dir))

def directed_loop(a, b, reverse):
	return range(a, b + 1) if not reverse else reversed(range(a, b + 1))
print(jinja.get_template("form.jinja.html").render(directed_loop=directed_loop))
