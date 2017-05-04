package it.uniroma3.kg.normalizer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import com.hp.hpl.jena.graph.Triple;

import it.uniroma3.configuration.Configuration;
import it.uniroma3.util.reader.RDFReader;
/**
 * 
 * @author matteo
 *
 */
public class TypesNormalizer extends NormalizerDBPediaDataset{
    
    public static void normalizeTypesFile(){
	System.out.println("Normalizing all DBPedia type-mapping files ...");
	long start_time = System.currentTimeMillis();
	try {
	    System.out.println("[main instance types]");
	    normalizeTypesDataset(Configuration.getSourceMainInstanceTypes(), Configuration.getIndexableDBPediaNormalizedTypesFile());
	    System.out.println("[lhd instance types]");
	    normalizeTypesDataset(Configuration.getSourceLHDInstanceTypes(), Configuration.getIndexableDBPediaLHDFile());
	    System.out.println("[dbtax instance types]");
	    normalizeTypesDataset(Configuration.getSourceDBTaxInstanceTypes(), Configuration.getIndexableDBPediaDBTaxFile());
	    System.out.println("[sdtyped instance types]");
	    normalizeTypesDataset(Configuration.getSourceSDTypedInstanceTypes(), Configuration.getIndexableDBPediaSDTypedFile());		
	} catch (IOException e) {
	    e.printStackTrace();
	}

	long end_time = System.currentTimeMillis();
	System.out.println(" done in " + TimeUnit.MILLISECONDS.toSeconds(end_time - start_time)  + " sec.");
    }

    private static void normalizeTypesDataset(String sourceBzip2File, String normalizedFile) throws IOException {
	String subject;
	String object;

	// first iteration: save second parts
	RDFReader reader = new RDFReader(sourceBzip2File, true);
	BufferedWriter bw = new BufferedWriter(new FileWriter(new File(normalizedFile)));

	Iterator<Triple> iter = reader.readTTLFile();
	while(iter.hasNext()){
	    Triple t = iter.next();
	    subject = t.getSubject().getURI();
	    object = t.getObject().getURI();

	    if (isDBPediaResource(subject) && !isIntermediateNode(subject) && isInDBPediaOntology(object)){
		bw.write(getResourceName(subject) + "\t" + getPredicateName(object));
		bw.write("\n");
	    }

	}
	bw.close();
	reader.closeReader();
    }

}