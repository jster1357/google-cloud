package io.cdap.plugin.gcp.bigquery.relational;

import io.cdap.cdap.etl.api.relational.Capability;
import io.cdap.cdap.etl.api.relational.Expression;
import io.cdap.cdap.etl.api.relational.ExpressionFactory;
import io.cdap.cdap.etl.api.relational.ExpressionFactoryType;
import io.cdap.cdap.etl.api.relational.ExtractableExpression;
import io.cdap.cdap.etl.api.relational.Relation;
import io.cdap.cdap.etl.api.relational.StringExpressionFactoryType;
import io.cdap.plugin.gcp.bigquery.sqlengine.builder.BigQueryBaseSQLBuilder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An {@link ExpressionFactory} that compiles SQL strings into expressions.
 * The resultant expressions are of type {@link SQLExpression}.
 */
public class SQLExpressionFactory implements ExpressionFactory<String> {

  /**
   * Gets the expression factory type, which in this case is SQL.
   *
   * @return {@link StringExpressionFactoryType}.SQL.
   */
  @Override
  public ExpressionFactoryType<String> getType() {
    return StringExpressionFactoryType.SQL;
  }

  /**
   * Saves the SQL expression specified in a {@link SQLExpression} and returns it.
   *
   * @param expression A valid SQL string with which an Expression can be created.
   * @return The compiled {@link SQLExpression}.
   */
  @Override
  public Expression compile(String expression) {
    return new SQLExpression(expression);
  }

  /**
   * Get the set of Capabilities supported, which in this case is SQL.
   *
   * @return A single capability, {@link StringExpressionFactoryType}.SQL.
   */
  @Override
  public Set<Capability> getCapabilities() {
    return new HashSet<>(Collections.singleton(StringExpressionFactoryType.SQL));
  }

  /**
   * Returns qualified dataset name for an expression. For BigQuery, dataset names are wrapped in quotes (`).
   * The resulting expression will be invalid if the relation is not a {@link BigQueryRelation}.
   * @param relation supplied relation.
   * @return valid containing the column name wrapped in quotes; or invalid expression.
   */
  @Override
  public ExtractableExpression<String> getQualifiedDataSetName(Relation relation) {
    // Ensure relation is BigQueryRelation
    if (!(relation instanceof BigQueryRelation)) {
      return new InvalidExtractableExpression("Relation is not BigQueryRelation");
    }

    // Return Dataset name wrapped in quotes.
    BigQueryRelation bqRelation = (BigQueryRelation) relation;
    String datasetName = bqRelation.getSourceDataset().getDatasetName();
    return new SQLExpression(qualify(datasetName));
  }

  /**
   * Returns qualified dataset column for a column. For BigQuery, dataset names are wrapped in quotes (`).
   * The resulting expression will be invalid if the relation is not a {@link BigQueryRelation}
   * or the column does not exist in this relation.
   * @param relation supplied relation.
   * @param column column name.
   * @return valid containing the column name wrapped in quotes; or invalid expression.
   */
  @Override
  public ExtractableExpression<String> getQualifiedColumnName(Relation relation, String column) {
    // Ensure relation is BigQueryRelation
    if (!(relation instanceof BigQueryRelation)) {
      return new InvalidExtractableExpression("Relation is not BigQueryRelation");
    }

    BigQueryRelation bqRelation = (BigQueryRelation) relation;

    // Ensure column is present in relation.
    if (!bqRelation.getColumns().contains(column)) {
      return new InvalidExtractableExpression("Column " + column + " is not present in dataset");
    }

    return new SQLExpression(qualify(column));
  }

  /**
   * Method used to build a qualified identified in BigQuery
   * @param identifier identifier
   * @return Identifier wrapped in quotes (`)
   */
  public String qualify(String identifier) {
    return BigQueryBaseSQLBuilder.QUOTE + identifier + BigQueryBaseSQLBuilder.QUOTE;
  }

  /**
   * Class used to return an invalid extractable expression.
   */
  protected static final class InvalidExtractableExpression implements ExtractableExpression<String> {
    String validationError;

    public InvalidExtractableExpression(String validationError) {
      this.validationError = validationError;
    }

    @Override
    public String extract() {
      return null;
    }

    @Override
    public boolean isValid() {
      return false;
    }

    @Override
    public String getValidationError() {
      return validationError;
    }
  }
}
