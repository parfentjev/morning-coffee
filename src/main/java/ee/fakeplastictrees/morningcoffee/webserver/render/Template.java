package ee.fakeplastictrees.morningcoffee.webserver.render;

interface Template {
  String renderToHtml(TemplateValue... values);
}
