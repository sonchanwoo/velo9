package teamexpress.velo9.member.security.handler;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public class CustomAccessDeniedHandler implements AccessDeniedHandler {

	private String errorPage;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {

		String deniedUrl = errorPage + "?exception=" + accessDeniedException.getMessage();
		response.sendRedirect(deniedUrl);
	}

	public void setErrorPage(String errorPage) {
		this.errorPage = errorPage;
	}
}
