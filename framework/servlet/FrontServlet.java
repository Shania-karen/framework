package framework.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.*;

public class FrontServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
       
        String context = req.getContextPath();        
        String urlPath = req.getRequestURI();
        String path=urlPath.substring(context.length());

        if(getRessource(path,req,resp)){
            return ;
        }        
        else{
            System.out.println(" a : " + urlPath);
        
            resp.setContentType("text/html");
            resp.getWriter().write("<h1> " + urlPath + "</h1>");
      
        }
    }
    private boolean getRessource( String path,HttpServletRequest req , HttpServletResponse resp) throws ServletException , IOException{
        String realPath=getServletContext().getRealPath(path);
        if(realPath !=null){
            File file= new File(realPath);
            if(file.exists() && file.isFile()){
                RequestDispatcher dispatcher=req.getRequestDispatcher(path);
                String mimeType=getServletContext().getMimeType(realPath);
                if(mimeType ==null){
                    mimeType="application/octet-stream";
                }
                resp.setContentType(mimeType);
                try {
                   FileInputStream in= new FileInputStream(file);
                   OutputStream out= resp.getOutputStream();
                   byte [] buffer=new byte[4096];
                   int read;
                   while((read=in.read(buffer))!=-1){
                    out.write(buffer , 0,read);
                   }
                } catch (Exception e) {
                 
                }
                return true;

            }
        }

        return false;
    }
}
