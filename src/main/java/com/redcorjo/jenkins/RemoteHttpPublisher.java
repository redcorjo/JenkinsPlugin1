package com.redcorjo.jenkins;

import com.redcorjo.shared.HttpSimpleRequest;
import com.redcorjo.shared.SharedAPIs;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.*;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;


public class RemoteHttpPublisher extends Notifier {

    private String parameters;
    private String headers;
    private String user;
    private String password;
    private String method;
    private String url;
    private String files;
    private boolean useheaders;
    private boolean useparameters;
    private boolean usecredentials;
    private boolean ignoreproxy;
    private boolean uploadfiles;

      // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public RemoteHttpPublisher(String parameters, String headers, String url, String method, String user, String password, String files,
                               Boolean useheaders, Boolean useparameters, Boolean usecredentials, Boolean ignoreproxy, Boolean uploadfiles) {

        this.method = method;
        this.url = url;
        this.useheaders = useheaders;
        this.useparameters = useparameters;
        this.usecredentials = usecredentials;
        this.uploadfiles = uploadfiles;
        this.ignoreproxy = ignoreproxy;

        if (useparameters) {
            this.parameters = parameters;
        }
        if ( useheaders ) {
            this.headers = headers;
        }
        if ( usecredentials) {
            this.user = user;
            this.password = password;
        }
        if ( uploadfiles) {
            this.files = files;
            this.method = "POST";
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * We'll use this from the {@code config.jelly}.
     */
    public String getParameters() {
        return parameters;
    }

    public String getHeaders() {return headers; }

    public String getUrl() {return url; }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getMethod() {
        if (uploadfiles) {
            return "POST";
        } else {
            return method;
        }
    }

    public boolean isUseheaders() {
        return useheaders;
    }

    public boolean isUseparameters() {
        return useparameters;
    }

    public boolean isUsecredentials() {
        return usecredentials;
    }

    public boolean isIgnoreproxy() {
        return ignoreproxy;
    }

    public void setIgnoreproxy(boolean ignoreproxy) {
        this.ignoreproxy = ignoreproxy;
    }

    public String getFiles() {
        return files;
    }

    public boolean isUploadfiles() {
        return uploadfiles;
    }

    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) {

        if (getDescriptor().getEnabled())
            listener.getLogger().println("This plugin is enabled");
        else {
            listener.getLogger().println("This plugin is disabled");
            return true;
        }

        String myworkspace = String.valueOf(build.getWorkspace());
        String buildLog = myworkspace + "/" + build.number + "/log";
        listener.getLogger().println("Build log is " + buildLog);

        Jenkins jenkins = Jenkins.getInstance();

        HttpSimpleRequest myrequest = new HttpSimpleRequest(url);

        try {
            if (jenkins.proxy.name != null && jenkins.proxy.port != 0 && isIgnoreproxy() == false) {
                myrequest.setProxyhost(jenkins.proxy.name);
                myrequest.setProxyport(jenkins.proxy.port);
            }
        } catch (Exception e){
            listener.getLogger().println("No proxy used");
        }
        if ( headers != null) {
            myrequest.setHeaders(headers);
        }
        if ( parameters != null) {
            myrequest.setParameters(parameters);
        }

        if (usecredentials) {
            myrequest.setCredentials(user,password);
        }

        if (uploadfiles) {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setIncludes(new String[]{files});
            scanner.setBasedir(myworkspace);
            scanner.setCaseSensitive(false);
            scanner.scan();
            String[] myfiles = scanner.getIncludedFiles();
            myrequest.setFiles(myfiles);
        }

        /*
        for (int i = 0; i < myfiles.length; i++) {
            System.out.println(myfiles[i]);
        }*/

        if ( method.equalsIgnoreCase("GET")) {
            myrequest.setMethod(myrequest.GET);
        } else if ( method.equalsIgnoreCase("POST")) {
            myrequest.setMethod(myrequest.POST);
        } else if ( method.equalsIgnoreCase("PUT")) {
            myrequest.setMethod(myrequest.PUT);
        } else if ( method.equalsIgnoreCase("DELETE")) {
            myrequest.setMethod(myrequest.DELETE);
        } else {
            myrequest.setMethod(myrequest.GET);
        }

        try {
             myrequest.myRequest();
           } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            listener.getLogger().println("Generic exception when launching the request");
        }
        listener.getLogger().println(myrequest.getCode());
        listener.getLogger().println(myrequest.getResult());
        listener.getLogger().println(myrequest.getResultjson());
        System.out.println("Myresult:" + myrequest.getResult());
        System.out.println("Mycode:" + myrequest.getCode());
        if ( myrequest.getCode() == 0) {
            return false;
        } else {
            return true;
        }
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use {@code transient}.
         */
        private boolean enabled;

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            super(RemoteHttpPublisher.class);
            load();
        }

        public FormValidation doCheckUrl(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.startsWith("http://") || value.startsWith("https://") )
                return FormValidation.ok();
            return FormValidation.error("Please enter an http or https URL");
        }

        public FormValidation doCheckUser(@QueryParameter String value) {
            if ( value.length() > 1 ) {
                return FormValidation.ok();
            }
            return FormValidation.error("Please enter a valid username");
        }

        public FormValidation doCheckPassword(@QueryParameter String value) {
            if ( value.length() > 1 ) {
                return FormValidation.ok();
            }
            return FormValidation.error("Please enter a valid password");
        }

        public FormValidation doCheckHeaders(@QueryParameter String value){
            if (SharedAPIs.checkJson(value)){
                return FormValidation.ok("String JSON Compatible");
            } else {
                return FormValidation.error("String NOT JSON Compatible. Try at https://jsonformatter.org");
            }
        }

        public FormValidation doCheckParameters(@QueryParameter String value){
            if (SharedAPIs.checkJson(value)){
                return FormValidation.ok("String JSON Compatible");
            } else {
                return FormValidation.error("String NOT JSON Compatible. Try at https://jsonformatter.org");
            }
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable parameters is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Remote Http Publisher";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            enabled = formData.getBoolean("enabled");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        public boolean getEnabled() {
            return enabled;
        }
    }
}

