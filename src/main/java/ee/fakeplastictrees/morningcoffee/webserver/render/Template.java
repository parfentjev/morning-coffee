package ee.fakeplastictrees.morningcoffee.webserver.render;

interface Template {
  String toHtml(TemplateData... values) throws TemplateException;
}
