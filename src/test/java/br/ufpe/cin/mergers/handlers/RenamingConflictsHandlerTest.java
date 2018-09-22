package br.ufpe.cin.mergers.handlers;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.BeforeClass;
import org.junit.Test;

import br.ufpe.cin.app.JFSTMerge;
import br.ufpe.cin.files.FilesManager;
import br.ufpe.cin.mergers.util.MergeContext;

public class RenamingConflictsHandlerTest {
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//hidding sysout output
		@SuppressWarnings("unused")
		PrintStream originalStream = System.out;
		PrintStream hideStream    = new PrintStream(new OutputStream(){
		    public void write(int b) {}
		});
		System.setOut(hideStream);
	}
	
	@Test
	public void testConflictingRenamingInLeft() {
		MergeContext ctx = 	new JFSTMerge().mergeFiles(
				new File("testfiles/renamingmethodleftconf/left.java"), 
				new File("testfiles/renamingmethodleftconf/base.java"), 
				new File("testfiles/renamingmethodleftconf/right.java"),
				null);
		String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);
		
		assertThat(mergeResult).contains("<<<<<<<MINEpublicvoidm(){inta;}=======publicvoidn(){}>>>>>>>YOURS");
		assertThat(ctx.renamingConflicts).isOne();
	}

	@Test
	public void testConflictingRenamingInRight() {
		MergeContext ctx = 	new JFSTMerge().mergeFiles(
				new File("testfiles/renamingmethodrightconf/left.java"), 
				new File("testfiles/renamingmethodrightconf/base.java"), 
				new File("testfiles/renamingmethodrightconf/right.java"),
				null);
		String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);
		
		assertThat(mergeResult).contains("<<<<<<<MINEpublicvoidm(){inta;}=======publicvoidn(){}>>>>>>>YOURS");
		assertThat(ctx.renamingConflicts).isOne();
	}
	
	@Test
	public void testNoConflictingRenamingInLeft() {
		MergeContext ctx = 	new JFSTMerge().mergeFiles(
				new File("testfiles/renamingmethodleftnoconf/left.java"), 
				new File("testfiles/renamingmethodleftnoconf/base.java"), 
				new File("testfiles/renamingmethodleftnoconf/right.java"),
				null);
		String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);
		
		assertThat(mergeResult).doesNotContain("(cause:possiblerenaming)");
		assertThat(ctx.renamingConflicts).isZero();
	}

	@Test
	public void testNoConflictingRenamingInRight() {
		MergeContext ctx = 	new JFSTMerge().mergeFiles(
				new File("testfiles/renamingmethodrightnoconf/left.java"), 
				new File("testfiles/renamingmethodrightnoconf/base.java"), 
				new File("testfiles/renamingmethodrightnoconf/right.java"),
				null);
		String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);
		
		assertThat(mergeResult).doesNotContain("(cause:possiblerenaming)");
		assertThat(ctx.renamingConflicts).isZero();
	}
	
	@Test
	public void testConflictingRenamingMutual() {
		MergeContext ctx = 	new JFSTMerge().mergeFiles(
				new File("testfiles/renamingmutual/left.java"), 
				new File("testfiles/renamingmutual/base.java"), 
				new File("testfiles/renamingmutual/right.java"),
				null);
		String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);

		assertThat(mergeResult).contains("<<<<<<<MINEintsum(inta,intb){returna+b;}=======intmySum(inta,intb){returna+b;}>>>>>>>YOURS");
		assertThat(ctx.renamingConflicts).isOne();
	}

	@Test
	public void testRenamingConflictsParameter() {
		boolean defaultValue = JFSTMerge.isRenamingConflictsHandlerEnabled;

		JFSTMerge.isRenamingConflictsHandlerEnabled = false;
		MergeContext ctx = 	new JFSTMerge().mergeFiles(
				new File("testfiles/renamingmethodleftconf/left.java"),
				new File("testfiles/renamingmethodleftconf/base.java"),
				new File("testfiles/renamingmethodleftconf/right.java"),
				null);
		String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);

		assertThat(mergeResult).isEqualTo("publicclassTest{publicvoidn(){}<<<<<<<MINE=======publicvoidm(){inta;}>>>>>>>YOURS}");
		assertThat(ctx.renamingConflicts).isZero();
		JFSTMerge.isRenamingConflictsHandlerEnabled = defaultValue;
	}
	

}
