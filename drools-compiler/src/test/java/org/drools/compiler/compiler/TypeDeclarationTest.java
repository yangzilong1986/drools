package org.drools.compiler.compiler;

import junit.framework.Assert;
import org.drools.core.common.EventFactHandle;
import org.drools.core.definitions.impl.KnowledgePackageImp;
import org.drools.core.rule.TypeDeclaration;
import org.junit.Test;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.KnowledgeBase;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.builder.KnowledgeBuilderResults;
import org.kie.internal.builder.ResultSeverity;
import org.kie.api.definition.type.Annotation;
import org.kie.api.definition.type.FactField;
import org.kie.api.definition.type.FactType;
import org.kie.internal.io.ResourceFactory;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.internal.runtime.StatefulKnowledgeSession;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class TypeDeclarationTest {

    @Test
    public void testClassNameClashing() {
        String str = "";
        str += "package org.kie \n" +
        		"declare org.kie.Character \n" +
        		"    name : String \n" +
        		"end \n";

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        kbuilder.add( ResourceFactory.newByteArrayResource( str.getBytes() ),
                      ResourceType.DRL );

        if ( kbuilder.hasErrors() ) {
           fail( kbuilder.getErrors().toString() );
        }
    }

    public void testAnnotationReDefinition(){
        String str1 = "";
        str1 += "package org.kie \n" +
        		"declare org.kie.EventA \n" +
        		"    name : String \n" +
        		"    duration : Long \n" +
        		"end \n";

        String str2 = "";
        str2 += "package org.kie \n" +
        		"declare org.kie.ClassA \n" +
        		"    @Role (event) \n" +
        		"    @duration (duration) \n" +
        		"end \n";

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        kbuilder.add( ResourceFactory.newByteArrayResource( str1.getBytes() ),
                      ResourceType.DRL );

        kbuilder.add( ResourceFactory.newByteArrayResource( str2.getBytes() ),
                      ResourceType.DRL );

        if (kbuilder.hasErrors() ) {
           fail( kbuilder.getErrors().toString() );
        }

        //No Warnings
        KnowledgeBuilderResults warnings = kbuilder.getResults(ResultSeverity.WARNING);
        Assert.assertEquals(0, warnings.size());

        //just 1 package was created
        Assert.assertEquals(1, kbuilder.getKnowledgePackages().size());

        //Get the Fact Type for org.kie.EventA
        FactType factType = ((KnowledgePackageImp)kbuilder.getKnowledgePackages().iterator().next()).pkg.getFactType("org.drools.EventA");
        assertNotNull( factType );

        //'name' field must still be there
        FactField field = factType.getField("name");
        assertNotNull( field );

        //'duration' field must still be there
        field = factType.getField("duration");
        assertNotNull( field );

        //New Annotations must be there too
        TypeDeclaration typeDeclaration = ((KnowledgePackageImp)kbuilder.getKnowledgePackages().iterator().next()).pkg.getTypeDeclaration("org.drools.EventA");

        assertEquals(TypeDeclaration.Role.EVENT, typeDeclaration.getRole());
        assertEquals("duration", typeDeclaration.getDurationAttribute());

    }

    public void testNoAnnotationUpdateIfError(){
        String str1 = "";
        str1 += "package org.drools.compiler \n" +
        		"declare org.drools.EventA \n" +
        		"    name : String \n" +
        		"    duration : Long \n" +
        		"end \n";

        String str2 = "";
        str2 += "package org.drools.compiler \n" +
        		"declare org.drools.ClassA \n" +
        		"    @Role (event) \n" +
        		"    @duration (duration) \n" +
        		"    anotherField : String \n" +
        		"end \n";

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        kbuilder.add( ResourceFactory.newByteArrayResource(str1.getBytes()),
                      ResourceType.DRL );

        kbuilder.add( ResourceFactory.newByteArrayResource( str2.getBytes() ),
                      ResourceType.DRL );

        if (!kbuilder.hasErrors() ) {
           fail("Errors Expected");
        }

        //No Warnings
        KnowledgeBuilderResults warnings = kbuilder.getResults(ResultSeverity.WARNING);
        Assert.assertEquals(0, warnings.size());

        //just 1 package was created
        Assert.assertEquals(1, kbuilder.getKnowledgePackages().size());

        //Get the Fact Type for org.drools.EventA
        FactType factType = ((KnowledgePackageImp)kbuilder.getKnowledgePackages().iterator().next()).pkg.getFactType("org.drools.EventA");
        assertNotNull( factType );

        //'name' field must still be there
        FactField field = factType.getField("name");
        assertNotNull( field );

        //'duration' field must still be there
        field = factType.getField("duration");
        assertNotNull( field );

        //@Role annotations shouldn't have any effect
        TypeDeclaration typeDeclaration = ((KnowledgePackageImp)kbuilder.getKnowledgePackages().iterator().next()).pkg.getTypeDeclaration("org.drools.EventA");

        assertEquals(TypeDeclaration.Role.FACT, typeDeclaration.getRole());
        assertNull(typeDeclaration.getDurationAttribute());

    }

    /**
     * The same resource (containing a type declaration) is added twice in the
     * kbuilder.
     */
    @Test
    public void testDuplicatedTypeDeclarationWith2FieldsInSameResource() {
        //same package, different resources
        String str1 = "";
        str1 += "package org.drools.compiler \n" +
        		"declare org.drools.ClassA \n" +
        		"    name : String \n" +
        		"    lastName : String \n" +
        		"end \n";

        Resource resource = ResourceFactory.newByteArrayResource( str1.getBytes());

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        kbuilder.add( resource, ResourceType.DRL );

        kbuilder.add( resource, ResourceType.DRL );

        if ( kbuilder.hasErrors() ) {
           fail( kbuilder.getErrors().toString() );
        }

    }

    /**
     * 2 resources (containing a the same type declaration) are added to the
     * kbuilder.
     * The expectation here is to silently discard the second type declaration.
     */
    @Test
    public void testDuplicatedTypeDeclarationInDifferentResources() {
        //same package, different resources
        String str1 = "";
        str1 += "package org.drools.compiler \n" +
        		"declare org.drools.ClassA \n" +
        		"    name : String \n" +
        		"end \n";

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        kbuilder.add( ResourceFactory.newByteArrayResource( str1.getBytes() ),
                      ResourceType.DRL );

        kbuilder.add( ResourceFactory.newByteArrayResource( str1.getBytes() ),
                      ResourceType.DRL );

        if ( kbuilder.hasErrors() ) {
           fail( kbuilder.getErrors().toString() );
        }

    }


    /**
     * 2 resources (containing different declarations of the same type ) are added
     * to the kbuilder.
     * The expectation here is that compilation fails because we are changing
     * the type of a field
     */
    @Test
    public void testClashingTypeDeclarationInDifferentResources() {
        //same package, different resources
        String str1 = "";
        str1 += "package org.drools.compiler \n" +
        		"declare org.drools.ClassA \n" +
        		"    name : String \n" +
        		"    age : Integer \n" +
        		"end \n";

        String str2 = "";
        str2 += "package org.drools.compiler \n" +
        		"declare org.drools.ClassA \n" +
        		"    name : String \n" +
        		"    age : String \n" +
        		"end \n";

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        kbuilder.add( ResourceFactory.newByteArrayResource( str1.getBytes() ),
                      ResourceType.DRL );

        kbuilder.add( ResourceFactory.newByteArrayResource( str2.getBytes() ),
                      ResourceType.DRL );

        if (!kbuilder.hasErrors() ) {
           fail( "An error should have been generated, redefinition of ClassA is not allowed" );
        }


    }

    /**
     * 2 resources (containing different declarations of the same type ) are added
     * to the kbuilder.
     * The expectation here is to silently discard the second type declaration.
     * This is because the new definition has less fields that the original
     * UPDATE : any use of the full-arg constructor in the second DRL will fail,
     * so we generate an error anyway
     */
    @Test
    public void testNotSoHarmlessTypeReDeclaration() {
        //same package, different resources
        String str1 = "";
        str1 += "package org.drools.compiler \n" +
        		"declare org.drools.ClassA \n" +
        		"    name : String \n" +
        		"    age : Integer \n" +
        		"end \n";

        String str2 = "";
        str2 += "package org.drools.compiler \n" +
        		"declare org.drools.ClassA \n" +
        		"    name : String \n" +
        		"end \n";

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        kbuilder.add( ResourceFactory.newByteArrayResource( str1.getBytes() ),
                      ResourceType.DRL );

        kbuilder.add( ResourceFactory.newByteArrayResource( str2.getBytes() ),
                      ResourceType.DRL );

        if ( ! kbuilder.hasErrors() ) {
           fail( "An error should have been generated, redefinition of ClassA is not allowed" );
        }

        /*
        //1 Warning
        KnowledgeBuilderResults warnings = kbuilder.getResults( ResultSeverity.WARNING );
        Assert.assertEquals(1, warnings.size());
        System.out.println(warnings.iterator().next().getMessage());

        //just 1 package was created
        Assert.assertEquals(1, kbuilder.getKnowledgePackages().size());

        //Get the Fact Type for org.drools.ClassA
        FactType factType = ((KnowledgePackageImp)kbuilder.getKnowledgePackages().iterator().next()).pkg.getFactType("org.drools.ClassA");
        Assert.assertNotNull(factType);

        //'age' field must still be there
        FactField field = factType.getField("age");
        Assert.assertNotNull(field);

        //Assert that the 'name' field must be String and not Long
        Assert.assertEquals(Integer.class, field.getType());
        */
    }

    /**
     * 2 resources (containing different declarations of the same type ) are added
     * to the kbuilder.
     * The expectation here is that the compilation fails because we are
     * adding a new field to the declared Type
     */
    @Test
    public void testTypeReDeclarationWithExtraField() {
        //same package, different resources
        String str1 = "";
        str1 += "package org.drools.compiler \n" +
        		"declare org.drools.ClassA \n" +
        		"    name : String \n" +
        		"    age : Integer \n" +
        		"end \n";

        String str2 = "";
        str2 += "package org.drools.compiler \n" +
        		"declare org.drools.ClassA \n" +
        		"    name : String \n" +
        		"    lastName : String \n" +
        		"end \n";

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        kbuilder.add( ResourceFactory.newByteArrayResource( str1.getBytes() ),
                      ResourceType.DRL );

        kbuilder.add( ResourceFactory.newByteArrayResource( str2.getBytes() ),
                      ResourceType.DRL );

        if ( ! kbuilder.hasErrors() ) {
           fail( kbuilder.getErrors().toString() );
        }


    }

    /**
     * 2 resources (containing different declarations of the same type ) are added
     * to the kbuilder.
     * The expectation here is that the compilation fails because we are
     * trying to add an incompatible re-definition of the declared type:
     * it introduces a new field 'lastName'
     */
    @Test
    public void testTypeReDeclarationWithExtraField2() {
        //same package, different resources
        String str1 = "";
        str1 += "package org.drools.compiler \n" +
        		"declare org.drools.ClassA \n" +
        		"    name : String \n" +
        		"    age : Integer \n" +
        		"end \n";

        String str2 = "";
        str2 += "package org.drools.compiler \n" +
        		"declare org.drools.ClassA \n" +
        		"    name : String \n" +
        		"    lastName : String \n" +
        		"end \n";

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        kbuilder.add( ResourceFactory.newByteArrayResource( str1.getBytes() ),
                      ResourceType.DRL );

        kbuilder.add( ResourceFactory.newByteArrayResource( str2.getBytes() ),
                      ResourceType.DRL );

        if (!kbuilder.hasErrors() ) {
           fail( kbuilder.getErrors().toString() );
        }

    }



    @Test
    public void testDuplicateDeclaration() {
        String str = "";
        str += "package org.drools.compiler \n" +
                "declare Bean \n" +
                "    name : String \n" +
                "end \n" +
                "declare Bean \n" +
                "    age : int \n" +
                "end \n";

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        kbuilder.add( ResourceFactory.newByteArrayResource( str.getBytes() ),
                ResourceType.DRL );

        if ( ! kbuilder.hasErrors() ) {
            fail( "Two definitions with the same name are not allowed, but it was not detected! " );
        }
    }



    @Retention(value = RetentionPolicy.RUNTIME)
    @Target(value = ElementType.TYPE)
    public static @interface KlassAnnotation {
        String value();
    }

    @Retention(value = RetentionPolicy.RUNTIME)
    @Target(value = ElementType.FIELD)
    public static @interface FieldAnnotation {
        String prop();
    }

    @Test
    public void testTypeDeclarationMetadata() {
        String str = "";
        str += "package org.drools.compiler.test; \n" +
                "import org.drools.compiler.compiler.TypeDeclarationTest.KlassAnnotation; \n" +
                "import org.drools.compiler.compiler.TypeDeclarationTest.FieldAnnotation; \n" +
                "import org.drools.compiler.Person\n" +
                "\n" +
                "declare Bean \n" +
                "@role(event) \n" +
                "@expires( 1s ) \n" +
                "@KlassAnnotation( \"klass\" )" +
                "" +
                "    name : String @key @FieldAnnotation( prop = \"fld\" )\n" +
                "end \n" +
                "declare Person @role(event) end";

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        kbuilder.add( ResourceFactory.newByteArrayResource( str.getBytes() ),
                ResourceType.DRL );
        System.err.println( kbuilder.getErrors() );
        assertFalse(kbuilder.hasErrors());

        KnowledgeBase kBase = KnowledgeBaseFactory.newKnowledgeBase();
        kBase.addKnowledgePackages( kbuilder.getKnowledgePackages() );

        FactType bean = kBase.getFactType( "org.drools.compiler.test", "Bean" );
        FactType pers = kBase.getFactType( "org.drools", "Person" );
        assertEquals( "org.drools.compiler.test.Bean", bean.getName() );
        assertEquals( "Bean", bean.getSimpleName() );
        assertEquals( "org.drools.compiler.test", bean.getPackageName() );

        assertEquals( 2, bean.getClassAnnotations().size() );
        Annotation ann = bean.getClassAnnotations().get( 0 );
        if (!ann.getName().equals("org.drools.compiler.compiler.TypeDeclarationTest$KlassAnnotation")) {
            ann = bean.getClassAnnotations().get( 1 );
        }
        assertEquals( "org.drools.compiler.compiler.TypeDeclarationTest$KlassAnnotation", ann.getName() );
        assertEquals( "klass", ann.getPropertyValue( "value" ) );
        assertEquals( String.class, ann.getPropertyType( "value" ) );

        assertEquals( 2, bean.getMetaData().size() );
        assertEquals( "event", bean.getMetaData().get( "role" ) );

        FactField field = bean.getField( "name" );
        assertNotNull( field );
        assertEquals( 1, field.getFieldAnnotations().size() );
        Annotation fnn = field.getFieldAnnotations().get( 0 );

        assertEquals( "org.drools.compiler.compiler.TypeDeclarationTest$FieldAnnotation", fnn.getName() );
        assertEquals( "fld", fnn.getPropertyValue( "prop" ) );
        assertEquals( String.class, fnn.getPropertyType( "prop" ) );

        assertEquals( 1, field.getMetaData().size() );
        assertTrue( field.getMetaData().containsKey( "key" ) );

    }

    public static class EventBar {
        public static class Foo {

        }
    }

    @Test
    public void testTypeDeclarationWithInnerClasses() {
        // DROOLS-150
        String str = "";
        str += "package org.drools.compiler;\n" +
               "\n" +
               "import org.drools.compiler.compiler.TypeDeclarationTest.EventBar.*;\n" +
               "" +
               "declare Foo\n" +
               " @role( event )\n" +
               "end\n" +
               "" +
               "rule R when Foo() then end";

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add( ResourceFactory.newByteArrayResource( str.getBytes() ),
                      ResourceType.DRL );
        System.err.println( kbuilder.getErrors() );
        assertFalse( kbuilder.hasErrors() );

        KnowledgeBase kBase = KnowledgeBaseFactory.newKnowledgeBase();
        kBase.addKnowledgePackages( kbuilder.getKnowledgePackages() );
        StatefulKnowledgeSession knowledgeSession = kBase.newStatefulKnowledgeSession();
        FactHandle handle = knowledgeSession.insert( new EventBar.Foo() );

        assertTrue( handle instanceof EventFactHandle );

    }

    @Test
    public void testTypeDeclarationWithInnerClassesImport() {
        // DROOLS-150
        String str = "";
        str += "package org.drools.compiler;\n" +
               "\n" +
               "import org.drools.compiler.compiler.TypeDeclarationTest.EventBar.Foo;\n" +
               "" +
               "declare Foo\n" +
               " @role( event )\n" +
               "end\n" +
               "" +
               "rule R when Foo() then end";

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add( ResourceFactory.newByteArrayResource( str.getBytes() ),
                      ResourceType.DRL );
        System.err.println( kbuilder.getErrors() );
        assertFalse( kbuilder.hasErrors() );

        KnowledgeBase kBase = KnowledgeBaseFactory.newKnowledgeBase();
        kBase.addKnowledgePackages( kbuilder.getKnowledgePackages() );
        StatefulKnowledgeSession knowledgeSession = kBase.newStatefulKnowledgeSession();
        FactHandle handle = knowledgeSession.insert( new EventBar.Foo() );

        assertTrue( handle instanceof EventFactHandle );

    }
}
