package core.mvc.asis;

import core.mvc.*;
import core.mvc.tobe.AnnotationHandlerMapping;
import core.mvc.tobe.HandlerExecution;
import core.mvc.tobe.HandlerMapping;
import core.mvc.tobe.HandlerMappingComposite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;

@WebServlet(name = "dispatcher", urlPatterns = "/", loadOnStartup = 1)
public class DispatcherServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(DispatcherServlet.class);
    private static final String DEFAULT_REDIRECT_PREFIX = "redirect:";
    private HandlerMapping handlerMapping;
    private ViewResolver viewResolver;

    @Override
    public void init() throws ServletException {
        initHandlerMapping();
        initViewResolver();
    }

    private void initHandlerMapping() {
        RequestMapping requestMapping = new RequestMapping();
        requestMapping.initMapping();
        AnnotationHandlerMapping annotationHandlerMapping = new AnnotationHandlerMapping("next");
        annotationHandlerMapping.initialize();

        handlerMapping = new HandlerMappingComposite(annotationHandlerMapping, requestMapping);
    }

    private void initViewResolver() {
        RedirectViewResolver redirectViewResolver = new RedirectViewResolver();
        JspViewResolver jspViewResolver = new JspViewResolver();
        viewResolver = new ViewResolverComposite(new LinkedHashSet<>(
                Arrays.asList(redirectViewResolver, jspViewResolver)
        ));
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestUri = req.getRequestURI();
        logger.debug("Method : {}, Request URI : {}", req.getMethod(), requestUri);

        HandlerExecution handler = handlerMapping.getHandler(req);

        try {
            ModelAndView modelAndView = handler.handle(req, resp);
            render(modelAndView, req, resp);
        } catch (Throwable e) {
            logger.error("Exception : {}", e);
            throw new ServletException(e.getMessage());
        }
    }

    private void move(String viewName, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (viewName.startsWith(DEFAULT_REDIRECT_PREFIX)) {
            resp.sendRedirect(viewName.substring(DEFAULT_REDIRECT_PREFIX.length()));
            return;
        }

        RequestDispatcher rd = req.getRequestDispatcher(viewName);
        rd.forward(req, resp);
    }

    private void render(ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response) throws Exception {

        View view;
        String viewName = modelAndView.getViewName();

        if (viewName == null) {
            view = (View) modelAndView.getView();
        } else {
            view = viewResolver.resolveViewName(viewName);
        }

        view.render(modelAndView.getModel(), request, response);
    }
}
