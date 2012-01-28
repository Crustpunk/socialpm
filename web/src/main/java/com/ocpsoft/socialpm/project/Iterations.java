/**
 * This file is part of OCPsoft SocialPM: Agile Project Management Tools (SocialPM) 
 *
 * Copyright (c)2011 Lincoln Baxter, III <lincoln@ocpsoft.com> (OCPsoft)
 * Copyright (c)2011 OCPsoft.com (http://ocpsoft.com)
 * 
 * If you are developing and distributing open source applications under 
 * the GNU General Public License (GPL), then you are free to re-distribute SocialPM 
 * under the terms of the GPL, as follows:
 *
 * SocialPM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SocialPM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SocialPM.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * For individuals or entities who wish to use SocialPM privately, or
 * internally, the following terms do not apply:
 *  
 * For OEMs, ISVs, and VARs who wish to distribute SocialPM with their 
 * products, or host their product online, OCPsoft provides flexible 
 * OEM commercial licenses.
 * 
 * Optionally, Customers may choose a Commercial License. For additional 
 * details, contact an OCPsoft representative (sales@ocpsoft.com)
 */
package com.ocpsoft.socialpm.project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.TransactionAttribute;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.jboss.seam.international.status.Messages;

import com.ocpsoft.socialpm.cdi.Web;
import com.ocpsoft.socialpm.domain.project.Project;
import com.ocpsoft.socialpm.domain.project.Velocity;
import com.ocpsoft.socialpm.domain.project.iteration.Iteration;
import com.ocpsoft.socialpm.domain.project.iteration.IterationStatistics;
import com.ocpsoft.socialpm.domain.project.stories.Story;
import com.ocpsoft.socialpm.domain.project.stories.Task;
import com.ocpsoft.socialpm.domain.user.Profile;
import com.ocpsoft.socialpm.model.project.IterationService;
import com.ocpsoft.socialpm.web.ParamsBean;

/**
 * @author <a href="mailto:bleathem@gmail.com">Brian Leathem</a>
 * 
 */
@Named
@ConversationScoped
public class Iterations implements Serializable
{
   private static final long serialVersionUID = -6828711689148386870L;

   private Messages messages;

   private Projects projects;

   private IterationService is;

   private ParamsBean params;

   public Iterations()
   {}

   @Inject
   public Iterations(final @Web EntityManager em, final IterationService is, final Projects projects,
            final Messages messages,
            final ParamsBean params)
   {
      this.params = params;
      this.messages = messages;
      this.projects = projects;
      this.is = is;
      this.is.setEntityManager(em);
   }

   private Iteration current = new Iteration();

   private final Iteration newiter = new Iteration();

   public String loadCurrent()
   {
      Project project = projects.getCurrent();

      if (project.isPersistent())
      {
         if ((current != null) && (params.getIterationNumber() != 0))
         {
            try {
               current = is.findByProjectAndNumber(project, params.getIterationNumber());
            }
            catch (NoResultException e) {
               messages.error("Oops! We couldn't find that Iteration.");
               return "/pages/iteration/sorter?faces-redirect=true&profile=" + project.getOwner().getUsername()
                        + "&project=" + project.getSlug() + "&iteration=1";
            }
         }
      }
      return null;
   }

   public int getAssignedTaskCount(final Profile p, final Iteration i)
   {
      return getAssignedTasks(p, i).size();
   }

   public List<Task> getAssignedTasks(final Profile p, final Iteration i)
   {
      List<Task> result = new ArrayList<Task>();
      // FIXME Super slow - query-ify this!
      for (Story s : i.getStories()) {
         for (Task t : s.getTasks()) {
            if (p.getUsername().equals(t.getAssignee().getUsername()))
               result.add(t);
         }
      }
      return result;
   }

   @TransactionAttribute
   public String create()
   {
      newiter.setStartDate(new Date());
      newiter.setEndDate(new Date());
      Iteration created = is.create(projects.getCurrent(), newiter);
      return "/pages/iteration/sorter?faces-redirect=true&project=" + projects.getCurrent().getSlug() + "&profile="
               + newiter.getProject().getOwner().getUsername()
               + "&iteration=" + is.getIterationNumber(created);
   }

   @TransactionAttribute
   public void saveAjax()
   {
      is.save(current);
   }

   @Produces
   @Named("iteration")
   @RequestScoped
   public Iteration getCurrent()
   {
      return current;
   }

   public void setCurrent(final Iteration current)
   {
      this.current = current;
   }

   @Produces
   @Named("newiteration")
   @RequestScoped
   public Iteration getNewIteration()
   {
      return newiter;
   }

   /* Velocity Visualization */
   public int getVelocityPercentage()
   {
      return 50;
   }

   public int getAllocationPercentage()
   {
      IterationStatistics commitmentStats = current.getCommitmentStats();
      Project project = projects.getCurrent();
      Velocity velocity = project.getVelocity();
      double allocated = commitmentStats.getHoursRemain() == 0 ? 1 : commitmentStats.getHoursRemain();
      double velocityHours = velocity.getHours();
      double result = (allocated / velocityHours) * 100;
      System.out.println(result);
      return (int) ((result / 100) * getVelocityPercentage());
   }
}