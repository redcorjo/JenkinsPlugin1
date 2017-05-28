package com.redcorjo.jenkins;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

import com.redcorjo.shared.HttpSimpleRequest;

public class RemoteHttpBuilder extends Builder implements SimpleBuildStep {

    private final String parameters;
    private final String headers;
    private final String url;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public RemoteHttpBuilder(String parameters, String headers, String url) {

        this.parameters = parameters;
        this.headers = headers;
        this.url = url;
    }

    /**
     * We'll use this from the {@code config.jelly}.
     */
    public String getParameters() {
        return parameters;
    }

    public String getHeaders() {return headers; }

    public String getUrl() {return url; }

    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) {

        if (getDescriptor().getEnabled())
            listener.getLogger().println("This plugin is enabled");
        else {
            listener.getLogger().println("This plugin is disabled");
            return;
        }

        String buildLog = workspace + "/" + build.number + "/log";
        listener.getLogger().println("Build log is " + buildLog);

        Jenkins jenkins = Jenkins.getInstance();

        HttpSimpleRequest myrequest = new HttpSimpleRequest(url);

        try {
            if (jenkins.proxy.name != null && jenkins.proxy.port != 0) {
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
        //myrequest.setNoProxyHost(jenkins.proxy.noProxyHost);

        try {
            //myresult = myrequest.myRequest();
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

    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }


    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
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
            super(RemoteHttpBuilder.class);
            load();
        }


        public FormValidation doCheckUrl(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.startsWith("http://") || value.startsWith("https://") )
                return FormValidation.ok();
            return FormValidation.error("Please enter an http or https URL");
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable parameters is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Remote Http builder";
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

