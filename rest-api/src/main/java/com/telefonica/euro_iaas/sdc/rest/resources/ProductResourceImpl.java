/**
 *   (c) Copyright 2013 Telefonica, I+D. Printed in Spain (Europe). All Rights
 *   Reserved.
 * 
 *   The copyright to the software program(s) is property of Telefonica I+D.
 *   The program(s) may be used and or copied only with the express written
 *   consent of Telefonica I+D or in accordance with the terms and conditions
 *   stipulated in the agreement/contract under which the program(s) have
 *   been supplied.
 */

package com.telefonica.euro_iaas.sdc.rest.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Path;

import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.multipart.BodyPartEntity;
import com.sun.jersey.multipart.MultiPart;
import com.telefonica.euro_iaas.commons.dao.EntityNotFoundException;
import com.telefonica.euro_iaas.sdc.exception.AlreadyExistsProductReleaseException;
import com.telefonica.euro_iaas.sdc.exception.InvalidMultiPartRequestException;
import com.telefonica.euro_iaas.sdc.exception.InvalidProductReleaseException;
import com.telefonica.euro_iaas.sdc.exception.InvalidProductReleaseUpdateRequestException;
import com.telefonica.euro_iaas.sdc.exception.ProductReleaseNotFoundException;
import com.telefonica.euro_iaas.sdc.exception.ProductReleaseStillInstalledException;
import com.telefonica.euro_iaas.sdc.exception.SdcRuntimeException;
import com.telefonica.euro_iaas.sdc.manager.ProductManager;
import com.telefonica.euro_iaas.sdc.model.Attribute;
import com.telefonica.euro_iaas.sdc.model.Product;
import com.telefonica.euro_iaas.sdc.model.ProductRelease;
import com.telefonica.euro_iaas.sdc.model.dto.ProductReleaseDto;
import com.telefonica.euro_iaas.sdc.model.dto.ReleaseDto;
import com.telefonica.euro_iaas.sdc.model.searchcriteria.ProductReleaseSearchCriteria;
import com.telefonica.euro_iaas.sdc.model.searchcriteria.ProductSearchCriteria;
import com.telefonica.euro_iaas.sdc.rest.validation.ProductResourceValidator;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * default ProductResource implementation
 * 
 * @author Sergio Arroyo
 */
@Path("/catalog/product")
@Component
@Scope("request")
public class ProductResourceImpl implements ProductResource {

    @InjectParam("productManager")
    private ProductManager productManager;

    private ProductResourceValidator validator;
    private static Logger LOGGER = Logger.getLogger("ProductResourceImpl");

    public ProductRelease insert(ProductReleaseDto productReleaseDto) throws AlreadyExistsProductReleaseException,
            InvalidProductReleaseException {
        LOGGER.info("Inserting a new product release in the software catalogue " + productReleaseDto.getProductName()
                + " " + productReleaseDto.getVersion() + " " + productReleaseDto.getProductDescription());
        Product product = new Product(productReleaseDto.getProductName(), productReleaseDto.getProductDescription());

        if (productReleaseDto.getPrivateAttributes() != null) {
            LOGGER.info("Attributes " + productReleaseDto.getPrivateAttributes().size());
            for (Attribute att : productReleaseDto.getPrivateAttributes())
                product.addAttribute(att);
        }

        ProductRelease productRelease = new ProductRelease(productReleaseDto.getVersion(),
                productReleaseDto.getReleaseNotes(), productReleaseDto.getPrivateAttributes(), product,
                productReleaseDto.getSupportedOS(), productReleaseDto.getTransitableReleases());
        LOGGER.info(productRelease.toString());
        return productManager.insert(productRelease);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws InvalidProductReleaseUpdateRequestException
     * @throws InvalidMultiPartRequestException
     */
    public ProductRelease insert(MultiPart multiPart) throws AlreadyExistsProductReleaseException,
            InvalidProductReleaseException, InvalidMultiPartRequestException {

        validator.validateInsert(multiPart);

        File cookbook = null;
        File installable = null;

        // First part contains a Project object
        ProductReleaseDto productReleaseDto = multiPart.getBodyParts().get(0).getEntityAs(ProductReleaseDto.class);
        LOGGER.log(Level.INFO, " Insert ProductRelease " + productReleaseDto.getProductName() + " version "
                + productReleaseDto.getVersion());

        Product product = new Product(productReleaseDto.getProductName(), productReleaseDto.getProductDescription());

        for (int i = 0; productReleaseDto.getPrivateAttributes().size() < 1; i++)
            product.addAttribute(productReleaseDto.getPrivateAttributes().get(i));

        ProductRelease productRelease = new ProductRelease(productReleaseDto.getVersion(),
                productReleaseDto.getReleaseNotes(), productReleaseDto.getPrivateAttributes(), product,
                productReleaseDto.getSupportedOS(), productReleaseDto.getTransitableReleases());

        try {
            cookbook = File.createTempFile(
                    "cookbook-" + productReleaseDto.getProductName() + "-" + productReleaseDto.getVersion() + ".tar",
                    ".tmp");

            installable = File.createTempFile("installable-" + productReleaseDto.getProductName() + "-"
                    + productReleaseDto.getVersion() + ".tar", ".tmp");

            cookbook = getFileFromBodyPartEntity((BodyPartEntity) multiPart.getBodyParts().get(1).getEntity(), cookbook);
            installable = getFileFromBodyPartEntity((BodyPartEntity) multiPart.getBodyParts().get(2).getEntity(),
                    installable);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return productManager.insert(productRelease, cookbook, installable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Product> findAll(Integer page, Integer pageSize, String orderBy, String orderType) {
        ProductSearchCriteria criteria = new ProductSearchCriteria();

        if (page != null && pageSize != null) {
            criteria.setPage(page);
            criteria.setPageSize(pageSize);
        }
        if (!StringUtils.isEmpty(orderBy)) {
            criteria.setOrderBy(orderBy);
        }
        if (!StringUtils.isEmpty(orderType)) {
            criteria.setOrderBy(orderType);
        }
        return productManager.findByCriteria(criteria);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Product load(String name) throws EntityNotFoundException {
        return productManager.load(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Attribute> loadAttributes(String name) throws EntityNotFoundException {
        return productManager.load(name).getAttributes();
    }

    /**
     * {@inheritDoc}
     */

    public List<ProductRelease> findAll(String name, String osType, Integer page, Integer pageSize, String orderBy,
            String orderType) {
        ProductReleaseSearchCriteria criteria = new ProductReleaseSearchCriteria();

        if (!StringUtils.isEmpty(name)) {
            try {
                Product product = productManager.load(name);
                criteria.setProduct(product);
            } catch (EntityNotFoundException e) {
                throw new SdcRuntimeException("Can not find the application " + name, e);
            }
        }

        if (!StringUtils.isEmpty(osType))
            criteria.setOSType(osType);

        if (page != null && pageSize != null) {
            criteria.setPage(page);
            criteria.setPageSize(pageSize);
        }
        if (!StringUtils.isEmpty(orderBy)) {
            criteria.setOrderBy(orderBy);
        }
        if (!StringUtils.isEmpty(orderType)) {
            criteria.setOrderBy(orderType);
        }
        return productManager.findReleasesByCriteria(criteria);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProductRelease load(String name, String version) throws EntityNotFoundException {
        Product product = productManager.load(name);
        return productManager.load(product, version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String name, String version) throws ProductReleaseNotFoundException,
            ProductReleaseStillInstalledException {

        LOGGER.log(Level.INFO, "Delete ProductRelease. ProductName : " + name + " ProductVersion : " + version);

        Product product;
        try {
            product = productManager.load(name);
        } catch (EntityNotFoundException e) {
            throw new ProductReleaseNotFoundException(e);
        }

        ProductRelease productRelease;
        try {
            productRelease = productManager.load(product, version);
        } catch (EntityNotFoundException e) {
            throw new ProductReleaseNotFoundException(e);
        }

        productManager.delete(productRelease);
    }

    public ProductRelease update(MultiPart multiPart) throws ProductReleaseNotFoundException,
            InvalidProductReleaseException, InvalidProductReleaseUpdateRequestException,
            InvalidMultiPartRequestException {

        ProductReleaseDto productReleaseDto = multiPart.getBodyParts().get(0).getEntityAs(ProductReleaseDto.class);
        LOGGER.log(Level.INFO,
                "ProductRelease " + productReleaseDto.getProductName() + " version " + productReleaseDto.getVersion());

        // TODO Validar el Objeto ProductReleaseDto en las validaciones
        Product product = new Product();
        ProductRelease productRelease = new ProductRelease();

        product.setName(productReleaseDto.getProductName());

        if (productReleaseDto.getProductDescription() != null)
            product.setDescription(productReleaseDto.getProductDescription());

        if (productReleaseDto.getPrivateAttributes() != null) {
            for (int i = 0; productReleaseDto.getPrivateAttributes().size() < 1; i++)
                product.addAttribute(productReleaseDto.getPrivateAttributes().get(i));
        }

        productRelease.setProduct(product);

        if (productReleaseDto.getVersion() != null)
            productRelease.setVersion(productReleaseDto.getVersion());

        // ReleaseNotes
        if (productReleaseDto.getReleaseNotes() != null)
            productRelease.setReleaseNotes(productReleaseDto.getReleaseNotes());

        // PrivateAttributes
        if (productReleaseDto.getPrivateAttributes() != null)
            productRelease.setPrivateAttributes(productReleaseDto.getPrivateAttributes());

        // SupportedOS
        if (productReleaseDto.getSupportedOS() != null)
            productRelease.setSupportedOOSS(productReleaseDto.getSupportedOS());

        // TransitableRelease
        if (productReleaseDto.getTransitableReleases() != null)
            productRelease.setTransitableReleases(productReleaseDto.getTransitableReleases());

        ReleaseDto releaseDto = new ReleaseDto(productReleaseDto.getProductName(), productReleaseDto.getVersion(),
                "product");

        validator.validateUpdate(releaseDto, multiPart);

        File cookbook = null;
        File installable = null;

        try {
            cookbook = File.createTempFile("cookbook-" + releaseDto.getName() + "-" + releaseDto.getVersion() + ".tar",
                    ".tmp");
            cookbook = getFileFromBodyPartEntity((BodyPartEntity) multiPart.getBodyParts().get(1).getEntity(), cookbook);
        } catch (IOException e) {
            throw new SdcRuntimeException(e);
        }

        try {
            installable = File.createTempFile("installable-" + releaseDto.getName() + "-" + releaseDto.getVersion()
                    + ".tar", ".tmp");

            installable = getFileFromBodyPartEntity((BodyPartEntity) multiPart.getBodyParts().get(2).getEntity(),
                    installable);
        } catch (IOException e) {
            throw new SdcRuntimeException(e);
        }

        return productManager.update(productRelease, cookbook, installable);
    }

    private File getFileFromBodyPartEntity(BodyPartEntity bpe, File file) {
        try {
            InputStream input = bpe.getInputStream();

            OutputStream out = new FileOutputStream(file);

            byte[] buf = new byte[1024];
            int len;
            while ((len = input.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            input.close();

        } catch (IOException e) {
            System.out.println("An error was produced : " + e.toString());
        }
        return file;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Attribute> loadAttributes(String name, String version) throws EntityNotFoundException {
        return load(name, version).getAttributes();
    }

    @Override
    public List<ProductRelease> findTransitable(String name, String version) throws EntityNotFoundException {
        return load(name, version).getTransitableReleases();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ProductRelease> findAllReleases(String osType, Integer page, Integer pageSize, String orderBy,
            String orderType) {
        ProductReleaseSearchCriteria criteria = new ProductReleaseSearchCriteria();
        if (!StringUtils.isEmpty(osType)) {
            criteria.setOSType(osType);
        }
        if (page != null && pageSize != null) {
            criteria.setPage(page);
            criteria.setPageSize(pageSize);
        }
        if (!StringUtils.isEmpty(orderBy)) {
            criteria.setOrderBy(orderBy);
        }
        if (!StringUtils.isEmpty(orderType)) {
            criteria.setOrderBy(orderType);
        }
        return productManager.findReleasesByCriteria(criteria);
    }

    /**
     * @param validator
     *            the validator to set
     */
    public void setValidator(ProductResourceValidator validator) {
        this.validator = validator;
    }

    /**
     * @param validator
     *            the validator to set
     */
    public void setProductManager(ProductManager productManager) {
        this.productManager = productManager;
    }

    public void delete(String name) throws ProductReleaseNotFoundException, ProductReleaseStillInstalledException {
        Product product;
        try {
            product = productManager.load(name);
        } catch (EntityNotFoundException e) {
            throw new ProductReleaseNotFoundException(e);
        }

        productManager.delete(product);

    }
}
