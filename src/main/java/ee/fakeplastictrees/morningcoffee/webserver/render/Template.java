package ee.fakeplastictrees.morningcoffee.webserver.render;

interface Template {
  String renderToHtml(TemplateData... values) throws TemplateException;
}
