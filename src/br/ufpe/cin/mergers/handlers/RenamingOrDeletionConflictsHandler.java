package br.ufpe.cin.mergers.handlers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import br.ufpe.cin.files.FilesManager;
import br.ufpe.cin.mergers.util.MergeConflict;
import br.ufpe.cin.mergers.util.MergeContext;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTTerminal;

public final class RenamingOrDeletionConflictsHandler {

	public static void handle(MergeContext context) {
		//possible renamings or deletions in left
		if(!context.deletedLeftNodes.isEmpty() || !context.deletedRightNodes.isEmpty()){
			for(Pair<String,FSTNode> tuple: context.deletedLeftNodes){
				List<Pair<Double,String>> similarNodes = new ArrayList<Pair<Double,String>>(); //list of possible nodes renaming a previous one
				if(nodeHasConflict(tuple.getRight())){
					String baseContent = tuple.getLeft();
					String currentNodeContent= ((FSTTerminal) tuple.getRight()).getBody(); //node content with conflict
					String editedNodeContent = FilesManager.extractMergeConflicts(currentNodeContent).get(0).right;
					for(FSTNode newNode : context.addedLeftNodes){ // a possible renamed node is seem as "new" node due to superimposition
						if(isValidNode(newNode)){
							String possibleRenamingContent = ((FSTTerminal) newNode).getBody();
							double similarity  	  		   = computeNodesContentSimilarity(baseContent, possibleRenamingContent);
							if(similarity >= 0.7){ //a typical value of 0.7 (up to 1.0) is used, increase it for a more accurate comparison, or decrease for a more relaxed one.
								Pair<Double,String> tp = Pair.of(similarity, possibleRenamingContent);
								similarNodes.add(tp);
							}
						}
					}
					if(similarNodes.isEmpty()){//there is no similar node. it is a possible deletion, so remove the conflict keeping the edited version of the content 
						FilesManager.findAndReplaceASTNodeContent(context.superImposedTree, currentNodeContent,editedNodeContent);
						
						//statistics
						context.deletionConflicts++;
					} else {
						String possibleRenamingContent = getMostSimilarContent(similarNodes);
						generateRenamingConflict(context, currentNodeContent, possibleRenamingContent, editedNodeContent,true);
					}
				}
			}

			//possible renamings or deletions in right
			for(Pair<String,FSTNode> tuple: context.deletedRightNodes){
				List<Pair<Double,String>> similarNodes = new ArrayList<Pair<Double,String>>(); //list of possible nodes renaming a previous one
				if(nodeHasConflict(tuple.getRight())){
					String baseContent = tuple.getLeft();
					String currentNodeContent= ((FSTTerminal) tuple.getRight()).getBody(); //node content with conflict
					String editedNodeContent = FilesManager.extractMergeConflicts(currentNodeContent).get(0).left;
					for(FSTNode newNode : context.addedRightNodes){ // a possible renamed node is seem as "new" node due to superimposition
						if(isValidNode(newNode)){
							String possibleRenamingContent = ((FSTTerminal) newNode).getBody();
							double similarity  	  		   = computeNodesContentSimilarity(baseContent, possibleRenamingContent);
							if(similarity >= 0.7){ //a typical value of 0.7 (up to 1.0) is used, increase it for a more accurate comparison, or decrease for a more relaxed one.
								Pair<Double,String> tp = Pair.of(similarity, possibleRenamingContent);
								similarNodes.add(tp);
							}
						}
					}
					if(similarNodes.isEmpty()){//there is no similar node. it is a possible deletion, so remove the conflict keeping the edited version of the content 
						FilesManager.findAndReplaceASTNodeContent(context.superImposedTree, currentNodeContent,editedNodeContent);
						
						//statistics
						context.deletionConflicts++;
					} else {
						String possibleRenamingContent = getMostSimilarContent(similarNodes);
						generateRenamingConflict(context, currentNodeContent, possibleRenamingContent, editedNodeContent,false);
					}
				}
			}
		}
	}

	/**
	 * Verifies if the content of two nodes matches based on the Levenshtein distance.
	 * The Levenshtein distance between two strings is defined as the minimum number of edits needed to transform 
	 * one string into the other, with the allowable edit operations being insertion, deletion, or substitution 
	 * of a single character. 
	 * @param previousNodeContent
	 * @param newNodeContent
	 * @return level of similarity up to 1.0
	 */
	private static double computeNodesContentSimilarity(String previousNodeContent,String newNodeContent) {
		@SuppressWarnings("unused")
		String longer = previousNodeContent, shorter = newNodeContent;
		if (previousNodeContent.length() < newNodeContent.length()) { // longer should always have greater length
			longer = newNodeContent; 
			shorter= previousNodeContent;
		}
		int longerLength = longer.length();
		if (longerLength == 0) {
			return 1.0; /* both strings are zero length */ 
		}

		int levenshteinDistance = StringUtils.getLevenshteinDistance(previousNodeContent, newNodeContent);
		return ((longerLength - levenshteinDistance)/(double) longerLength);

	}

	private static String getMostSimilarContent(List<Pair<Double, String>> similarNodes) {
		similarNodes.sort((n1, n2) -> n1.getLeft().compareTo(n2.getLeft()));		
		return (similarNodes.get(similarNodes.size()-1)).getRight();// the top of the list
	}

	private static boolean nodeHasConflict(FSTNode node) {
		if(isValidNode(node)){
			String body = ((FSTTerminal) node).getBody();
			return body.contains("<<<<<<< LEFT");
		}
		return false;
	}

	private static boolean isValidNode(FSTNode node) {
		if(node instanceof FSTTerminal){
			String nodeType = ((FSTTerminal)node).getType();
			if(nodeType.equals("MethodDecl") || nodeType.equals("ConstructorDecl")){
				return true;
			}
		}
		return false;
	}

	private static void generateRenamingConflict(MergeContext context,String currentNodeContent, String firstContent,String secondContent, boolean isLeftToRight) {
		if(!isLeftToRight){//managing the origin of the changes in the conflict
			String aux 	 = secondContent;
			secondContent= firstContent;
			firstContent = aux;
		}

		//first creates a conflict 
		MergeConflict newConflict = new MergeConflict(firstContent+'\n', secondContent+'\n', "(cause: possible renaming)");
		//second put the conflict in one of the nodes containing the previous conflict, and deletes the other node containing the possible renamed version
		FilesManager.findAndReplaceASTNodeContent(context.superImposedTree, currentNodeContent, newConflict.body);
		if(isLeftToRight){
			FilesManager.findAndDeleteASTNode(context.superImposedTree, firstContent);
		} else {
			FilesManager.findAndDeleteASTNode(context.superImposedTree, secondContent);

		}
		
		//statistics
		context.renamingConflicts++;
	}
}
