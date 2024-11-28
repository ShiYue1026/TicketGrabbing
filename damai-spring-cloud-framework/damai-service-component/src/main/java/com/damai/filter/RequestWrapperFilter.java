package com.damai.filter;

/*
当我们调用getInputStream()方法获取输入流时，得到的是一个InputStream对象，而实际类型是ServletInputStream，它继承InputStream。

查看InputStream的源码可以看到(这里就不贴代码，大家有兴趣可以去找具体的源码部分)，读取流的时候会根据position来获取当前位置，每读取一次，该标志就会移动一次，
如果读到最后，read()返回-1，表示已经读取完了。如果想要重新读取，可以调用inputstream.reset方法，但是能否reset取决于markSupported方法，返回true可以reset，
反之则不行。查看ServletInputStream可知，这个类并没有重写markSupported和reset方法。

综上，InputStream默认不实现reset方法，而ServletInputStream也没有重写reset相关方法，这样就无法重复读取流，这就是我们从request对象中输入流只能读取一次的原因。
*/

import com.damai.request.CustomizeRequestWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RequestWrapperFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        CustomizeRequestWrapper requestWrapper = new CustomizeRequestWrapper(request);
        filterChain.doFilter(requestWrapper, response);
    }
}
