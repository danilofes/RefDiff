package refdiff.parsers.java;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;

import org.junit.Test;

import refdiff.core.diff.RastRootHelper;
import refdiff.core.io.SourceFolder;
import refdiff.core.rast.Location;
import refdiff.core.rast.RastNode;
import refdiff.core.rast.RastNodeRelationship;
import refdiff.core.rast.RastNodeRelationshipType;
import refdiff.core.rast.RastRoot;
import refdiff.parsers.RastParser;

public class TestJavaParser {
	
	private RastParser parser = new JavaParserNoBindings();
	
	@Test
	public void shouldParseFiles() throws Exception {
		Path basePath = Paths.get("test-data/parser/java");
		SourceFolder sources = SourceFolder.from(basePath, Paths.get("p2/Foo.java"), Paths.get("p1/Bar.java"));
		
		RastRoot root = parser.parse(sources);
		
		assertThat(root.getNodes().size(), is(2));
		
		RastNode classFoo = root.getNodes().get(0);
		assertThat(classFoo.getType(), is(NodeTypes.CLASS_DECLARATION));
		assertThat(classFoo.getNamespace(), is("p2."));
		assertThat(classFoo.getLocalName(), is("Foo"));
		assertThat(classFoo.getSimpleName(), is("Foo"));
		assertThat(classFoo.getLocation(), is(new Location("p2/Foo.java", 13, 121)));
		
		assertThat(classFoo.getNodes().size(), is(1));
		RastNode fooM1 = classFoo.getNodes().get(0);
		assertThat(fooM1.getType(), is(NodeTypes.METHOD_DECLARATION));
		assertThat(fooM1.getLocalName(), is("m1(String)"));
		assertThat(fooM1.getParameters().size(), is(1));
		assertThat(fooM1.getParameters().get(0).getName(), is("arg"));
		assertThat(fooM1.getSimpleName(), is("m1"));
		assertThat(fooM1.getLocation(), is(new Location("p2/Foo.java", 35, 117, 111, 116)));
		
		RastNode classBar = root.getNodes().get(1);
		assertThat(classBar.getType(), is(NodeTypes.CLASS_DECLARATION));
		assertThat(classBar.getNamespace(), is("p1."));
		assertThat(classBar.getLocalName(), is("Bar"));
		assertThat(classBar.getSimpleName(), is("Bar"));
		
		assertThat(classBar.getNodes().size(), is(2));
		RastNode barM1 = classBar.getNodes().get(0);
		assertThat(barM1.getType(), is(NodeTypes.METHOD_DECLARATION));
		assertThat(barM1.getLocalName(), is("m1(String)"));
		assertThat(barM1.getSimpleName(), is("m1"));
		
		RastNode barM2 = classBar.getNodes().get(1);
		assertThat(barM2.getType(), is(NodeTypes.METHOD_DECLARATION));
		assertThat(barM2.getLocalName(), is("m2()"));
		assertThat(barM2.getSimpleName(), is("m2"));
		
		Set<RastNodeRelationship> relationships = root.getRelationships();
		assertThat(relationships.size(), is(2));
		assertThat(relationships, hasItem(rel(RastNodeRelationshipType.USE, barM1, barM2)));
		assertThat(relationships, hasItem(rel(RastNodeRelationshipType.SUBTYPE, classBar, classFoo)));
		
		String fooSourceCode = sources.readContent(sources.getSourceFiles().get(0));
		assertThat(
			RastRootHelper.retrieveTokens(root, fooSourceCode, fooM1, false),
			is(Arrays.asList("/**", "A", "javadoc", "comment.", "@param", "arg", "*/", "public", "void", "m1", "(", "String", "arg", ")", "{", "}"))
		);
		
		String barSourceCode = sources.readContent(sources.getSourceFiles().get(1));
		assertThat(
			RastRootHelper.retrieveTokens(root, barSourceCode, barM1, false),
			is(Arrays.asList("public", "void", "m1", "(", "String", "arg", ")", "{", "m2", "(", ")", ";", "}"))
		);
		
		assertThat(
			RastRootHelper.retrieveTokens(root, barSourceCode, barM1, true),
			is(Arrays.asList("m2", "(", ")", ";"))
		);
	}
	
	private RastNodeRelationship rel(RastNodeRelationshipType type, RastNode n1, RastNode n2) {
		return new RastNodeRelationship(type, n1.getId(), n2.getId());
	}
}
