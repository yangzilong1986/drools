package org.kie.scanner;

import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.ReleaseId;
import org.kie.internal.builder.IncrementalResults;
import org.kie.internal.builder.InternalKieBuilder;

import static junit.framework.Assert.*;

public class KieModuleIncrementalCompilationTest extends AbstractKieCiTest {

    @Test
    public void testIncrementalCompilationFirstBuildHasErrors() throws Exception {
        KieServices ks = KieServices.Factory.get();

        //Malformed POM - No Version information
        ReleaseId releaseId = ks.newReleaseId( "org.kie", "incremental-test-with-invalid pom", "" );

        KieFileSystem kfs = createKieFileSystemWithKProject( ks );
        kfs.writePomXML( getPom( releaseId ) );

        //Valid
        String drl1 =
                "rule R1 when\n" +
                "   $s : String()\n" +
                "then\n" +
                "end\n";

        //Invalid
        String drl2 =
                "rule R2 when\n" +
                "   $s : Strin( )\n" +
                "then\n" +
                "end\n";

        //Write Rule 1 - No DRL errors, but POM is in error
        kfs.write( "src/main/resources/KBase1/r1.drl", drl1 );
        KieBuilder kieBuilder = ks.newKieBuilder( kfs ).buildAll();
        assertEquals( 1,
                      kieBuilder.getResults().getMessages( org.kie.api.builder.Message.Level.ERROR ).size() );

        //Add file with error - expect 1 "added" error message
        kfs.write( "src/main/resources/KBase1/r2.drl", drl2 );
        IncrementalResults addResults = ( (InternalKieBuilder) kieBuilder ).createFileSet( "src/main/resources/KBase1/r2.drl" ).build();

        assertEquals( 1, addResults.getAddedMessages().size() );
        assertEquals( 0, addResults.getRemovedMessages().size() );
    }

}
