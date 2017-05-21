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

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link RemoteHttpBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #parameters})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
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
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.

        // This also shows how you can consult the global configuration of the builder
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

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link RemoteHttpBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See {@code src/main/resources/hudson/plugins/hello_world/RemoteHttpBuilder/*.jelly}
     * for the actual HTML fragment for the configuration screen.
     */
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

        /**
         * Performs on-the-fly validation of the form field 'parameters'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckUrl(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.startsWith("http"))
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

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method parameters is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public boolean getEnabled() {
            return enabled;
        }
    }
}

